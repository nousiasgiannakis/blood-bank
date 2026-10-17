import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PlusOutlined } from '@ant-design/icons';
import {
  AppButtonPrimary,
  AppLink,
  AppTag,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import { animalsApi, type Animal, type OwnerInfo } from '../api';

/**
 * The animals list — built on @helex/ui's ResourceList, the same component the
 * production Helex applications use for every list page. Columns, search, an
 * action button, and a detail side panel showing the owner fetched through the
 * (mock or X-Road) registry adapter.
 */
export const AnimalList = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [animals, setAnimals] = useState<Animal[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Animal | null>(null);
  const [owner, setOwner] = useState<OwnerInfo | null>(null);

  const load = useCallback((text: string) => {
    setLoading(true);
    animalsApi
      .list(text || undefined)
      .then((result) => setAnimals(result.data))
      .catch((e) => notify.error('Could not load animals', e.message))
      .finally(() => setLoading(false));
  }, [notify]);

  useEffect(() => {
    const t = setTimeout(() => load(search), 250);
    return () => clearTimeout(t);
  }, [search, load]);

  useEffect(() => {
    setOwner(null);
    if (selected?.id && selected.ownerIsikukood) {
      animalsApi
        .owner(selected.id)
        .then(setOwner)
        .catch(() => setOwner(null)); // registry down: show nothing, not an error page
    }
  }, [selected]);

  const columns: ResourceListColumn<Animal>[] = [
    {
      key: 'registryCode',
      title: 'Registry code',
      dataIndex: 'registryCode',
      locked: true,
      // The identifier is the way into the record: it links to the animal's page.
      render: (code: string, animal: Animal) => (
        <AppLink
          href={`/animals/${animal.id}`}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate(`/animals/${animal.id}`);
          }}
        >
          {code}
        </AppLink>
      ),
    },
    { key: 'name', title: 'Name', dataIndex: 'name' },
    {
      key: 'speciesCode',
      title: 'Species',
      dataIndex: 'speciesCode',
      render: (code: string) => <AppTag>{code}</AppTag>,
    },
    { key: 'birthDate', title: 'Born', dataIndex: 'birthDate' },
    { key: 'chipNumber', title: 'Chip', dataIndex: 'chipNumber', defaultVisible: false },
  ];

  return (
    <ResourceList<Animal>
      title="Animals"
      columns={columns}
      dataSource={animals}
      rowKey="id"
      loading={loading}
      search={{ value: search, onChange: setSearch, placeholder: 'Name or registry code…' }}
      actions={
        <AppButtonPrimary icon={<PlusOutlined />} onClick={() => navigate('/animals/new')}>
          Register animal
        </AppButtonPrimary>
      }
      detailView={{
        selectedRecord: selected,
        onSelectedRecordChange: setSelected,
        title: 'Animal',
        render: (animal) => (
          <div style={{ display: 'grid', gap: 8 }}>
            <div><b>{animal.name}</b> · {animal.registryCode}</div>
            <div>Species: {animal.speciesCode}</div>
            {animal.birthDate && <div>Born: {animal.birthDate}</div>}
            {animal.chipNumber && <div>Chip: {animal.chipNumber}</div>}
            {animal.ownerIsikukood && (
              <div>
                Owner: {animal.ownerIsikukood}
                {owner && (
                  <>
                    {' — '}
                    <b>{owner.firstName} {owner.lastName}</b>, {owner.address}
                    <div style={{ opacity: 0.65, fontSize: 12 }}>
                      via the owner-registry adapter (mock / X-Road)
                    </div>
                  </>
                )}
              </div>
            )}
          </div>
        ),
      }}
    />
  );
};
