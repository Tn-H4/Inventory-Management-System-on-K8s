import { z } from 'zod';

export const purchaseSchema = z.object({
  productId: z
    .number({ invalid_type_error: 'You must select a product' })
    .min(1, 'You must select a product'),
  supplierId: z
    .number({ invalid_type_error: 'You must select a supplier' })
    .min(1, 'You must select a supplier'),
  quantity: z
    .number({ invalid_type_error: 'Quantity must be a number' })
    .int('Quantity must be a whole number')
    .min(1, 'Quantity must be at least 1'),
  description: z.string().optional(),
  note: z.string().optional(),
});

export const purchaseFormDefaultValues = {
  productId: 0,
  supplierId: 0,
  quantity: 1,
  description: '',
  note: '',
};
