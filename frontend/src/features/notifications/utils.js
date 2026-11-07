export const getTypeStyles = (type) => {
  switch (type) {
    case 'WARNING':
      return 'bg-warning-light text-warning';
    case 'ERROR':
      return 'bg-error-light text-error';
    case 'SUCCESS':
      return 'bg-green-100 text-green-700';
    default:
      return 'bg-blue-100 text-blue-700';
  }
};

export const getTypeBorder = (type) => {
  switch (type) {
    case 'WARNING':
      return 'border-warning';
    case 'ERROR':
      return 'border-red-500';
    case 'SUCCESS':
      return 'border-green-500';
    default:
      return 'border-primary';
  }
};
