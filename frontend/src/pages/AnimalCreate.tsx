import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Select, DatePicker } from 'antd';
import { AppButtonPrimary, AppButtonSecondary, AppCard, useAppNotification } from '@helex/ui';
import type { Dayjs } from 'dayjs';
import { animalsApi } from '../api';

/**
 * Registration form. Field-by-field this follows the UI specification in
 * docs/specifications/ANIMALS.01 § UI — every input is a library component,
 * per the mapping rule from session 4.
 */
export const AnimalCreate = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [species, setSpecies] = useState<{ code: string; name: string }[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    animalsApi.species().then(setSpecies).catch((e) => notify.error('Could not load species', e.message));
  }, [notify]);

  const submit = (values: {
    registryCode: string;
    name: string;
    speciesCode: string;
    birthDate?: Dayjs;
    ownerIsikukood?: string;
    chipNumber?: string;
  }) => {
    setSaving(true);
    animalsApi
      .create({
        registryCode: values.registryCode,
        name: values.name,
        speciesCode: values.speciesCode,
        birthDate: values.birthDate?.format('YYYY-MM-DD'),
        ownerIsikukood: values.ownerIsikukood || undefined,
        chipNumber: values.chipNumber || undefined,
      })
      .then((created) => {
        notify.success('Registered', `${created.name} · ${created.registryCode}`);
        navigate('/animals');
      })
      .catch((e) => notify.error('Registration failed', e.message)) // 400/409 problem detail, verbatim
      .finally(() => setSaving(false));
  };

  return (
    <AppCard title="Register a new animal" style={{ maxWidth: 640, margin: '24px auto' }}>
      <Form layout="vertical" onFinish={submit} requiredMark>
        <Form.Item
          name="registryCode"
          label="Registry code"
          rules={[{ required: true, max: 50 }]}
          extra="Unique among active animals — e.g. EE-2026-0042"
        >
          <Input placeholder="EE-2026-0042" />
        </Form.Item>
        <Form.Item name="name" label="Name" rules={[{ required: true, max: 255 }]}>
          <Input placeholder="Muri" />
        </Form.Item>
        <Form.Item name="speciesCode" label="Species" rules={[{ required: true }]}>
          <Select
            options={species.map((s) => ({ value: s.code, label: s.name }))}
            placeholder="Choose a species"
          />
        </Form.Item>
        <Form.Item name="birthDate" label="Birth date" extra="Must not be in the future">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="ownerIsikukood"
          label="Owner personal code"
          rules={[{ pattern: /^\d{11}$/, message: 'Exactly 11 digits' }]}
          extra="Try 38102130265 — the mock registry knows this person"
        >
          <Input placeholder="38102130265" />
        </Form.Item>
        <Form.Item name="chipNumber" label="Microchip number" rules={[{ max: 30 }]}>
          <Input />
        </Form.Item>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <AppButtonSecondary onClick={() => navigate('/animals')}>Cancel</AppButtonSecondary>
          <AppButtonPrimary htmlType="submit" loading={saving}>
            Register
          </AppButtonPrimary>
        </div>
      </Form>
    </AppCard>
  );
};
