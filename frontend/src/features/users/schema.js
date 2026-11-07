import { z } from 'zod';

export const userSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z
    .string()
    .trim()
    .optional()
    .refine(
      (v) => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
      'Invalid email format'
    ),
  phoneNumber: z.string().min(1, 'Phone number is required'),
  password: z.string().trim().optional(),
  role: z.enum(['ADMIN', 'MANAGER', 'STAFF']).default('STAFF'),
});

export const userFormDefaultValues = {
  name: '',
  email: '',
  phoneNumber: '',
  password: '',
  role: 'STAFF',
};
