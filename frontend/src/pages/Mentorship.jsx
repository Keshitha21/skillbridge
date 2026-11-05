import { useEffect, useState } from 'react'
import { PageHeading, Panel, StatusBadge, Field, EmptyState, useToast, Spinner } from '../components'
import { get, post } from '../api'

/** U8 / F13 — mentorship & skill exchange offers, requests and feedback. */
export default function Mentorship() {
  const toast = useToast()
  const [offers, setOffers] = useState(null)
  const [requests, setRequests] = useState([])
  const [skills, setSkills] = useState([])
  const [filters, setFilters] = useState({ type: '', status: '', q: '' })
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ type: 'OFFER', title: '', description: '', skillId: '', maxParticipants: 2 })

  const load = () => {
    const qs = new URLSearchParams()
    if (filters.type) qs.set('type', filters.type)
    if (filters.status) qs.set('status', filters.status)
    if (filters.q) qs.set('q', filters.q)
    Promise.allSettled([
      get(`/api/mentorship?${qs}`),
      get('/api/mentorship/requests'),
      get('/api/skills'),
    ]).then(([o, r, s]) => {
      if (o.status === 'fulfilled') setOffers(o.value)
      if (r.status === 'fulfilled') setRequests(r.value)
      if (s.status === 'fulfilled') setSkills(s.value)
    })
  }
  useEffect(() => { load() }, [filters])

  const requestJoin = async (offer) => {
    const message = prompt(`Message to the host of "${offer.title}":`, '')
    if (message === null) return
    try {
      await post(`/api/mentorship/${offer.id}/requests`, { message })
      toast('Request sent ✉️'); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const decide = async (requestId, action) => {
    try {
      await post(`/api/mentorship/requests/${requestId}/${action}`, {})
      toast(action === 'accept' ? 'Accepted 🤝' : 'Declined')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const complete = async (requestId) => {
    try {
      await post(`/api/mentorship/requests/${requestId}/complete`, {})
      toast('Marked complete — feedback is now open ⭐')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const createOffer = async (e) => {
    e.preventDefault()
    const data = new FormData(e.target)
    const body = {
      type: data.get('type'), title: data.get('title'), description: data.get('description'),
      skillId: data.get('skillId') ? Number(data.get('skillId')) : null,
      maxParticipants: Number(data.get('maxParticipants') || 1),
    }
    try {
      await post('/api/mentorship', body)
      toast('Created 🎯')
      setShowForm(false); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  if (offers === null) return <Spinner />

  return (
    <>
      <PageHeading title="Mentorship & skill exchange" lead="Offer what you know, request what you want to learn (U8 / F13)."
        action={<button className="btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close form' : '+ New listing'}</button>} />

      {showForm && (
        <Panel title="Create a mentorship or skill-swap listing" className="mb-6">
          <form onSubmit={createOffer} className="grid gap-4 md:grid-cols-2">
            <Field label="I am…">
              <select className="field-input" name="type">
                <option value="OFFER">Offering to teach / mentor</option>
                <option value="REQUEST">Requesting help</option>
              </select>
            </Field>
            <Field label="Related skill">
              <select className="field-input" name="skillId">
                <option value="">— any skill —</option>
                {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="Title">
              <input className="field-input" name="title" required maxLength={120} placeholder="Spring Boot basics 1:1" />
            </Field>
            <Field label="Max participants">
              <input className="field-input" type="number" name="maxParticipants" min="1" max="20" defaultValue="2" />
            </Field>
            <div className="md:col-span-2">
              <Field label="Description">
                <textarea className="field-input" name="description" rows={3} placeholder="What will the sessions cover? When?" />
              </Field>
            </div>
            <div className="md:col-span-2">
              <button className="btn-primary">Publish listing</button>
            </div>
          </form>
        </Panel>
      )}

      <div className="mb-8 grid gap-5 lg:grid-cols-2">
        <Panel title="Requests on my listings">
          {requests.length === 0 ? <EmptyState icon="📨" text="No mentorship requests yet." />
            : <ul className="divide-y divide-line text-sm">
              {requests.map(r => (
                <li key={r.id} className="py-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <span><strong>{r.requesterName}</strong> → <em>{r.offerTitle}</em></span>
                    <StatusBadge value={r.status} />
                  </div>
                  {r.message && <p className="mt-1 rounded-lg bg-pagebg px-2 py-1 text-xs">“{r.message}”</p>}
                  <div className="mt-1.5 flex flex-wrap gap-1.5">
                    {r.status === 'PENDING' && <>
                      <button className="btn-primary btn-sm" onClick={() => decide(r.id, 'accept')}>Accept</button>
                      <button className="btn-danger btn-sm" onClick={() => decide(r.id, 'decline')}>Decline</button>
                    </>}
                    {r.status === 'ACCEPTED' && <button className="btn-secondary btn-sm" onClick={() => complete(r.id)}>Mark completed</button>}
                  </div>
                </li>
              ))}
            </ul>}
        </Panel>
        <Panel title="My requests">
          {requests.length === 0 ? <EmptyState icon="🗂️" text="Requests you send will be tracked here." />
            : <ul className="divide-y divide-line text-sm">
              {requests.map(r => (
                <li key={`mine-${r.id}`} className="py-2.5">
                  <div className="flex items-center justify-between">
                    <span><em>{r.offerTitle}</em> — hosted by {r.offerOwnerName}</span>
                    <StatusBadge value={r.status} />
                  </div>
                  {r.status === 'ACCEPTED' && <button className="btn-secondary btn-sm mt-1.5" onClick={() => complete(r.id)}>Mark completed</button>}
                </li>
              ))}
            </ul>}
        </Panel>
      </div>

      <div className="mb-5 flex flex-wrap gap-2">
        {[['', 'All listings'], ['OFFER', 'Offers to mentor'], ['REQUEST', 'Requests for help']].map(([v, label]) => (
          <button key={v} className={`${filters.type === v ? 'btn-primary' : 'btn-secondary'} btn-sm`} onClick={() => setFilters({ ...filters, type: v })}>{label}</button>
        ))}
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {offers.map(o => (
          <Panel key={o.id}>
            <div className="mb-2 flex items-start justify-between gap-2">
              <h3 className="font-display font-bold text-ink">{o.title}</h3>
              <span className={`badge ${o.type === 'OFFER' ? 'badge-green' : 'badge-amber'}`}>{o.type === 'OFFER' ? 'Teaching' : 'Wants help'}</span>
            </div>
            <p className="mb-3 line-clamp-3 text-sm text-muted">{o.description}</p>
            <div className="flex flex-wrap gap-1.5">
              {o.skillName && <span className="tag">{o.skillName}</span>}
              <StatusBadge value={o.status} />
            </div>
            <div className="mt-3 text-xs text-muted">👤 {o.ownerName} · {o.activeParticipants}/{o.maxParticipants} spots filled</div>
            <button className="btn-primary btn-sm mt-3" onClick={() => requestJoin(o)}>Request to join</button>
          </Panel>
        ))}
        {offers.length === 0 && <EmptyState text="No listings match these filters." />}
      </div>
    </>
  )
}
