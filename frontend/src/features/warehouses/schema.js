import { z } from 'zod';

export const warehouseSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  location: z.string().min(1, 'Location is required'),
  address: z.string().min(1, 'Address is required'),
  capacity: z.string().min(1, 'Capacity is required'),
  managerName: z.string().min(1, "Manager's name is required"),
  status: z.enum(['ACTIVE', 'INACTIVE']).default('ACTIVE'),
});

export const warehouseFormDefaultValues = {
  name: '',
  location: '',
  address: '',
  capacity: '0',
  managerName: '',
  status: 'ACTIVE',
};
