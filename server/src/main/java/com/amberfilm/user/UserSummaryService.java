package com.amberfilm.user;

import java.util.List;
import java.util.Map;
import com.amberfilm.member.MemberAssetService;
import com.amberfilm.member.MemberDtos.MemberAssetSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserSummaryService {
  private static final List<LevelRule> LEVELS = List.of(
      new LevelRule("普通会员", 0),
      new LevelRule("银牌会员", 1000),
      new LevelRule("金牌会员", 5000),
      new LevelRule("黑金会员", 10000));

  private final JdbcTemplate jdbcTemplate;
  private final MemberAssetService memberAssetService;

  public UserSummaryService(JdbcTemplate jdbcTemplate, MemberAssetService memberAssetService) {
    this.jdbcTemplate = jdbcTemplate;
    this.memberAssetService = memberAssetService;
  }

  public UserSummaryDto summary(long userId) {
    Map<String, Object> row = jdbcTemplate.queryForMap("""
        SELECT COUNT(*) AS order_count,
               COALESCE(SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END), 0) AS completed_order_count,
               COALESCE(SUM(CASE WHEN pay_status = 'paid' THEN price_cent ELSE 0 END), 0) AS paid_amount_cent
        FROM orders
        WHERE user_id = ? AND user_hidden = FALSE
        """, userId);

    int growth = toInt(row.get("paid_amount_cent")) / 100;
    int orderCount = toInt(row.get("order_count"));
    int completedOrderCount = toInt(row.get("completed_order_count"));
    LevelRule current = resolveCurrentLevel(growth);
    LevelRule next = resolveNextLevel(growth);
    MemberAssetSummaryDto assets = memberAssetService.summary(userId);

    return new UserSummaryDto(
        current.name(),
        next == null ? "" : next.name(),
        growth,
        next == null ? 0 : Math.max(0, next.threshold() - growth),
        assets.couponCount(),
        assets.pointBalance(),
        assets.balanceCent(),
        assets.cardCount(),
        orderCount,
        completedOrderCount);
  }

  private LevelRule resolveCurrentLevel(int growth) {
    LevelRule current = LEVELS.get(0);
    for (LevelRule level : LEVELS) {
      if (growth >= level.threshold()) {
        current = level;
      }
    }
    return current;
  }

  private LevelRule resolveNextLevel(int growth) {
    for (LevelRule level : LEVELS) {
      if (growth < level.threshold()) {
        return level;
      }
    }
    return null;
  }

  private static int toInt(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return 0;
  }

  private record LevelRule(String name, int threshold) {
  }
}
