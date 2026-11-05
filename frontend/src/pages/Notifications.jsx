import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, EmptyState, useToast, Spinner } from '../components'
import { get, post } from '../api'

/** U7 / F12 — notification centre: list, mark one read, mark all read. */
export default function Notifications() {
  const toast = useToast()
  const [data, setData] = useState(null)

  const load = () => get('/api/notifications').then(setData).catch(() => setData({ items: [], unreadCount: 0 }))
  useEffect(() => { load() }, [])

  if (!data) return <Spinner />

  const markRead = async (id) => {
    try { await post(`/api/notifications/${id}/read`, {}); load() }
    catch (ex) { toast({ text: ex.message }) }
  }
  const markAll = async () => {
    try { await post('/api/notifications/read-all', {}); toast('All marked as read ✅'); load() }
    catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <PageHeading title="Notifications" lead={`${data.unreadCount} unread of ${data.items.length} total.`}
        action={<button className="btn-secondary" onClick={markAll} disabled={data.unreadCount === 0}>Mark all read</button>} />

      {data.items.length === 0
        ? <EmptyState icon="🔔" text="Nothing here yet — activity on your projects and mentorships will show up." />
        : <Panel>
          <ul className="divide-y divide-line">
            {data.items.map(n => (
              <li key={n.id} className={`flex items-start justify-between gap-4 py-3 ${n.read ? '' : 'bg-soft/40 -mx-3 rounded-lg px-3'}`}>
                <div>
                  <div className="flex items-center gap-2">
                    <strong className="text-ink">{n.title}</strong>
                    {!n.read && <span className="badge badge-red">new</span>}
                  </div>
                  {n.body && <p className="mt-0.5 text-sm text-muted">{n.body}</p>}
                  <span className="text-[11px] text-[#8993a5]">{new Date(n.createdAt).toLocaleString()}</span>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  {n.link && <Link className="btn-ghost btn-sm" to={n.link}>Open</Link>}
                  {!n.read && <button className="btn-secondary btn-sm" onClick={() => markRead(n.id)}>✓ read</button>}
                </div>
              </li>
            ))}
          </ul>
        </Panel>}
    </>
  )
}
