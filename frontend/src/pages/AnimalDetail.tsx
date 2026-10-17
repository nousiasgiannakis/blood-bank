import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { DatePicker, Form } from 'antd';
import dayjs from 'dayjs';
import { IdcardOutlined } from '@ant-design/icons';
import { useDataController } from '@helex/core';
import {
  AppCard,
  AppDate,
  AppTag,
  AppTimestamp,
  FieldItem,
  ResourceForm,
  useAppNotification,
  type ResourceFormSection,
} from '@helex/ui';
import { animalsApi, type Animal, type OwnerInfo } from '../api';

/**
 * A date input that accepts what the form store holds. ResourceForm's built-in
 * `date` field hands the picker a dayjs, but antd's Form.Item then injects the
 * store value over it — and the store holds the API's `YYYY-MM-DD` string, which
 * crashes the picker. This wrapper normalises in both directions: string or
 * dayjs in, `YYYY-MM-DD` string out — so the store, the API and the data
 * controller all speak strings.
 */
const DateField = ({ value, onChange }: { value?: unknown; onChange?: (next: string | null) => void }) => (
  <DatePicker
    style={{ width: '100%' }}
    value={value ? dayjs(String(value)) : null}
    onChange={(d) => onChange?.(d ? d.format('YYYY-MM-DD') : null)}
    maxDate={dayjs()}
  />
);

/**
 * One animal, on @helex/ui's ResourceForm — the same view/edit page pattern the
 * production Helex applications use for every record, paired with the platform's
 * useDataController. View mode reads the loaded record; Edit switches the same
 * sections to inputs; Save is `PUT /api/animals/{id}` (the registry code stays
 * read-only — it is identity); Retire is the soft `DELETE`. The sidebar is the
 * platform's metadata card — who created and changed the record, and when — as in
 * @helex/ui's "Resource Form › With Sidebar (metadata)" story; the owner's name
 * and address from the registry adapter appear beside the personal code.
 */
export const AnimalDetail = () => {
  const { id } = useParams();
  const animalId = Number(id);
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [form] = Form.useForm<Animal>();
  const dc = useDataController<Animal>({} as Animal);
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [species, setSpecies] = useState<{ code: string; name: string }[]>([]);
  const [owner, setOwner] = useState<OwnerInfo | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([animalsApi.get(animalId), animalsApi.species()])
      .then(([animal, list]) => {
        if (cancelled) return;
        dc.load(animal);
        form.setFieldsValue(animal);
        setSpecies(list);
        if (animal.ownerIsikukood) {
          animalsApi.owner(animalId).then((o) => !cancelled && setOwner(o)).catch(() => setOwner(null));
        }
      })
      .catch((e) => {
        notify.error('Could not load the animal', e.message);
        navigate('/animals');
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // dc, form and navigate are stable handles; the record is keyed by the route parameter.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animalId, notify]);

  const save = async (values: Animal) => {
    setSaving(true);
    try {
      const updated = await animalsApi.update(animalId, { ...values, registryCode: dc.current.registryCode });
      dc.load(updated);
      form.setFieldsValue(updated);
      setMode('view');
      notify.success('Saved', `${updated.name} · ${updated.registryCode}`);
    } catch (e) {
      notify.error('Save failed', (e as Error).message); // 400/409 problem detail, verbatim
    } finally {
      setSaving(false);
    }
  };

  const retire = async () => {
    setDeleting(true);
    try {
      await animalsApi.retire(animalId);
      notify.success('Retired', `${dc.current.name} · ${dc.current.registryCode}`);
      navigate('/animals');
    } catch (e) {
      notify.error('Retire failed', (e as Error).message);
      setDeleting(false);
    }
  };

  const sections: ResourceFormSection<Animal>[] = [
    {
      key: 'identity',
      title: 'Identity',
      fields: [
        {
          name: 'registryCode',
          label: 'Registry code',
          type: 'text',
          modeOverride: 'view', // immutable after registration — the API ignores it on PUT
          tooltip: 'Identity does not change; retire and re-register to reuse a code',
        },
        { name: 'name', label: 'Name', type: 'text', required: true, rules: [{ required: true, max: 255 }] },
        {
          name: 'speciesCode',
          label: 'Species',
          type: 'select',
          required: true,
          options: species.map((s) => ({ value: s.code, label: s.name })),
          viewRender: (code) => <AppTag>{String(code ?? '')}</AppTag>,
        },
      ],
    },
    {
      key: 'details',
      title: 'Details',
      fields: [
        {
          name: 'birthDate',
          label: 'Birth date',
          type: 'custom',
          tooltip: 'Must not be in the future',
          render: ({ value, onChange }) => <DateField value={value} onChange={onChange} />,
          viewRender: (value) => (value ? <AppDate value={String(value)} /> : '—'),
        },
        {
          name: 'ownerIsikukood',
          label: 'Owner personal code',
          type: 'text',
          rules: [{ pattern: /^\d{11}$/, message: 'Exactly 11 digits' }],
          placeholder: '38102130265',
          // View mode adds what the registry adapter knows about that code — never stored.
          viewRender: (code) =>
            code ? (
              <>
                {String(code)}
                {owner && (
                  <>
                    {' — '}
                    <b>
                      {owner.firstName} {owner.lastName}
                    </b>
                    , {owner.address}
                    <div style={{ opacity: 0.65, fontSize: 12 }}>via the owner-registry adapter (mock / X-Road)</div>
                  </>
                )}
              </>
            ) : (
              '—'
            ),
        },
        { name: 'chipNumber', label: 'Microchip number', type: 'text', rules: [{ max: 30 }] },
      ],
    },
  ];

  return (
    <ResourceForm<Animal>
      mode={mode}
      entityLabel="Animal"
      icon={<IdcardOutlined />}
      name={dc.current.name}
      subtitle={dc.current.registryCode}
      chips={dc.current.speciesCode ? <AppTag>{dc.current.speciesCode}</AppTag> : undefined}
      loading={loading}
      saving={saving}
      deleting={deleting}
      form={form}
      dataController={dc}
      initialValues={dc.current}
      sections={sections}
      onBack={() => navigate('/animals')}
      backTooltip="Back to the list"
      onEdit={() => setMode('edit')}
      onCancel={() => {
        dc.reset();
        form.setFieldsValue(dc.original ?? dc.current);
        setMode('view');
      }}
      onSave={save}
      onDelete={retire}
      deleteLabel="Retire"
      deleteConfirmText="Retire this animal? A soft delete — the registry code becomes reusable."
      sidebar={
        <AppCard title="Metadata" size="small">
          <FieldItem label="Created at">
            <AppTimestamp value={dc.current.sysCreatedAt} fallback="—" />
          </FieldItem>
          <FieldItem label="Created by">{dc.current.sysCreatedBy ?? '—'}</FieldItem>
          <FieldItem label="Modified at">
            <AppTimestamp value={dc.current.sysModifiedAt} fallback="—" />
          </FieldItem>
          <FieldItem label="Modified by">{dc.current.sysModifiedBy ?? '—'}</FieldItem>
          <FieldItem label="Version">{dc.current.sysVersion ?? '—'}</FieldItem>
        </AppCard>
      }
    />
  );
};
