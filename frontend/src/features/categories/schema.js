import { z } from 'zod';

export const categorySchema = z.object({
  name: z.string().min(1, 'Category name is required'),
  description: z.string().trim().optional(),
  status: z.enum(['ACTIVE', 'INACTIVE']).default('ACTIVE'),
});

export const categoryFormDefaultValues = {
  name: '',
  description: '',
  status: 'ACTIVE',
};
