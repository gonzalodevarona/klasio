import { render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import MembershipStatusBadge from '@/components/memberships/MembershipStatusBadge';

const messages = {
  badges: {
    membershipStatus: {
      EXPIRED: 'Expired',
      INACTIVE: 'Inactive',
      PENDING_PAYMENT: 'Pending Payment',
      PENDING_PAYMENT_VALIDATION: 'Under Review',
      PENDING_MANAGER_ACTIVATION: 'Pending Activation',
      ACTIVE: 'Active',
    },
  },
};

function wrap(ui: React.ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

it('renders ACTIVE label', () => {
  wrap(<MembershipStatusBadge status="ACTIVE" />);
  expect(screen.getByText('Active')).toBeInTheDocument();
});

it('renders EXPIRED label', () => {
  wrap(<MembershipStatusBadge status="EXPIRED" />);
  expect(screen.getByText('Expired')).toBeInTheDocument();
});

it('renders PENDING_PAYMENT label', () => {
  wrap(<MembershipStatusBadge status="PENDING_PAYMENT" />);
  expect(screen.getByText('Pending Payment')).toBeInTheDocument();
});
