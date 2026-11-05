import { useEffect, useState } from 'react'
import { PageHeading, Panel, Field, EmptyState, useToast, Spinner } from '../components'
import { get, post } from '../api'

/** Feedback after completed mentorship sessions (F13 tail): give once per offer, see what you received. */
export default function Feedback() {
  const toast = useToast()
  const [received, setReceived] = useState([])
  const [offers, setOffers] = useState([])
  const [myRequests, setMyRequests] = useState([])
  const [givenFor, setGivenFor] = useState({})
  const [loading, setLoading] = useState(true)

  const load = () => {
    Promise.allSettled([
      get('/api/feedback/received'),
      get('/api/mentorship'),
      get('/api/mentorship/requests'),
    ]).then(([r, o, m]) => {
      if (r.status === 'fulfilled') setReceived(r.value)
      if (o.status === 'fulfilled') setOffers(o.value)
      if (m.status === 'fulfilled') setMyRequests(m.value)
      setLoading(false)
    })
  }
  useEffect(() => { load() }, [])

  // eligible = completed activities where I am participant or owner, and I haven't given feedback yet
  const eligible = myRequests.filter(r => r.status === 'COMPLETED')

  const give = async (e, offerId) => {
    e.preventDefault()
    const data = new FormData(e.target)
    try {
      await post(`/api/mentorship/${offerId}/feedback`, {
        rating: Number(data.get('rating')), comment: data.get('comment'),
      })
      toast('Feedback sent ⭐')
      setGivenFor((g) => ({ ...g, [offerId]: true }))
    } catch (ex) { toast({ text: ex.message }) }
  }

  if (loading) return <Spinner />

  return (
    <>
      <PageHeading title="Peer feedback" lead="Rate the learning sessions you shared — honest feedback keeps exchanges healthy." />

      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title="Give feedback">
          {eligible.length === 0
            ? <EmptyState icon="⏳" text="Feedback unlocks once a mentorship session is marked completed." />
            : <div className="space-y-4">
              {eligible.map(r => givenFor[r.offerId] ? null : (
                <form key={r.id} onSubmit={(e) => give(e, r.offerId)} className="rounded-xl bg-pagebg p-4">
                  <strong className="block text-ink">{r.offerTitle}</strong>
                  <span className="text-xs text-muted">with {r.offerOwnerId && r.requesterId ? (r.offerOwnerName) : ''}</span>
                  <div className="mt-3 grid gap-3 md:grid-cols-[110px_1fr]">
                    <Field label="Rating">
                      <select className="field-input" name="rating" defaultValue="5">
                        <option value="5">★★★★★</option><option value="4">★★★★</option>
                        <option value="3">★★★</option><option value="2">★★</option><option value="1">★</option>
                      </select>
                    </Field>
                    <Field label="Comment">
                      <input className="field-input" name="comment" maxLength={600} placeholder="What went well?" />
                    </Field>
                  </div>
                  <button className="btn-primary btn-sm">Send feedback</button>
                </form>
              ))}
              {eligible.every(r => givenFor[r.offerId]) && <EmptyState icon="✅" text="You've reviewed all completed sessions." />}
            </div>}
        </Panel>

        <Panel title="Feedback you received">
          {received.length === 0
            ? <EmptyState icon="🌱" text="No feedback yet — complete a session and your partner can rate it." />
            : <ul className="divide-y divide-line">
              {received.map(f => (
                <li key={f.id} className="py-3 text-sm">
                  <div className="flex items-center justify-between">
                    <strong>{f.fromName}</strong>
                    <span className="text-ink">{'★'.repeat(f.rating)}<span className="text-[#d4dbe6]">{'★'.repeat(5 - f.rating)}</span></span>
                  </div>
                  {f.comment && <p className="mt-1 text-muted">“{f.comment}”</p>}
                </li>
              ))}
            </ul>}
        </Panel>
      </div>
    </>
  )
}
