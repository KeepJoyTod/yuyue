import { request } from '@/api/request';
import type { LoginResponseDto, UserDto } from '@/types/api';
import type { AuthUser } from '@/store/useAuthStore';

const toAuthUser = (dto: UserDto): AuthUser => ({
  id: String(dto.id),
  nickName: dto.nickName,
  phone: dto.phone,
  realName: dto.realName,
  avatarUrl: dto.avatarUrl
});

export async function loginByPhone(phone: string, code: string) {
  const result = await request<LoginResponseDto>('/api/auth/phone-login', {
    method: 'POST',
    data: { phone, code }
  });
  return {
    token: result.token,
    user: toAuthUser(result.user)
  };
}

export async function fetchCurrentUser() {
  const user = await request<UserDto>('/api/users/me');
  return toAuthUser(user);
}

export async function updateRealName(realName: string) {
  const user = await request<UserDto>('/api/users/real-name', {
    method: 'POST',
    data: { realName }
  });
  return toAuthUser(user);
}
