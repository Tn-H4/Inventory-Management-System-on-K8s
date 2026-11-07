import { z } from 'zod';

export const profileSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Enter a valid email address'),
  phoneNumber: z.string().min(1, 'Phone number is required'),
});

export const profileFormDefaultValues = {
  name: '',
  email: '',
  phoneNumber: '',
};
