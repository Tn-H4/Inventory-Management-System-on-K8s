import { z } from 'zod';

export const supplierSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  contactInfo: z.string().min(1, 'Contact info is required'),
  address: z.string().min(1, 'Address is required'),
  email: z
    .string()
    .trim()
    .optional()
    .refine(
      (v) => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
      'Invalid email format'
    ),
  website: z.string().trim().optional(),
  taxCode: z.string().trim().optional(),
  status: z.enum(['ACTIVE', 'INACTIVE']).default('ACTIVE'),
});

export const supplierFormDefaultValues = {
  name: '',
  contactInfo: '',
  address: '',
  email: '',
  website: '',
  taxCode: '',
  status: 'ACTIVE',
};
