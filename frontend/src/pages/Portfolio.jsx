import { useEffect, useState } from 'react'
import { PageHeading, Panel, Field, StatusBadge, EmptyState, useToast, Spinner } from '../components'
import { get, post, put, del } from '../api'

/** U8 / F14 — showcase completed projects & achievements, with public/private visibility. */
export default function Portfolio() {
  const toast = useToast()
  const [items, setItems] = useState(null)
  const [myProjects, setMyProjects] = useState([])
  const [editing, setEditing] = useState(null) // item being edited or 'new'

  const load = () => {
    Promise.allSettled([get('/api/portfolio/mine'), get('/api/projects?mine=true')])
      .then(([p, m]) => {
        if (p.status === 'fulfilled') setItems(p.value)
        if (m.status === 'fulfilled') setMyProjects(m.value)
      })
  }
  useEffect(() => { load() }, [])

  const save = async (e) => {
    e.preventDefault()
    const d = new FormData(e.target)
    const body = {
      title: d.get('title'), description: d.get('description'),
      tag: d.get('tag'), link: d.get('link'),
      visibility: d.get('visibility'),
      projectId: d.get('projectId') ? Number(d.get('projectId')) : null,
    }
    try {
      if (editing && editing !== 'new') await put(`/api/portfolio/${editing.id}`, body)
      else await post('/api/portfolio', body)
      toast(editing && editing !== 'new' ? 'Entry updated ✅' : 'Added to your portfolio 🏆')
      setEditing(null); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const remove = async (id) => {
    try { await del(`/api/portfolio/${id}`); toast('Deleted'); load() }
    catch (ex) { toast({ text: ex.message }) }
  }

  if (items === null) return <Spinner />

  return (
    <>
      <PageHeading title="My portfolio" lead="Completed projects, achievements and certifications (U8 / F14)."
        action={<button className="btn-primary" onClick={() => setEditing(editing ? null : 'new')}>
          {editing ? 'Close form' : '+ New entry'}</button>} />

      {editing && (
        <Panel title={editing === 'new' ? 'New portfolio entry' : `Edit: ${editing.title}`} className="mb-6">
          <form onSubmit={save} className="grid gap-4 md:grid-cols-2">
            <Field label="Title">
              <input className="field-input" name="title" required maxLength={150}
                     defaultValue={editing === 'new' ? '' : editing.title} />
            </Field>
            <Field label="Type">
              <select className="field-input" name="tag" defaultValue={editing === 'new' ? 'PROJECT' : editing.tag}>
                <option value="PROJECT">Project</option>
                <option value="ACHIEVEMENT">Achievement</option>
                <option value="CERTIFICATION">Certification</option>
              </select>
            </Field>
            <Field label="Link to finished project">
              <select className="field-input" name="projectId" defaultValue={editing === 'new' ? '' : (editing.projectId || '')}>
                <option value="">— not linked —</option>
                {myProjects.map(p => <option key={p.id} value={p.id}>{p.title}</option>)}
              </select>
            </Field>
            <Field label="External link (optional)">
              <input className="field-input" name="link" placeholder="https://github.com/…" defaultValue={editing === 'new' ? '' : (editing.link || '')} />
            </Field>
            <div className="md:col-span-2">
              <Field label="Description">
                <textarea className="field-input" name="description" rows={3} defaultValue={editing === 'new' ? '' : (editing.description || '')} />
              </Field>
            </div>
            <Field label="Visibility" hint="Private entries are only visible to you (and admins).">
              <select className="field-input" name="visibility" defaultValue={editing === 'new' ? 'PUBLIC' : editing.visibility}>
                <option value="PUBLIC">Public</option>
                <option value="PRIVATE">Private</option>
              </select>
            </Field>
            <div className="flex items-end">
              <button className="btn-primary">Save entry</button>
            </div>
          </form>
        </Panel>
      )}

      {items.length === 0 ? <EmptyState icon="🏆" text="Nothing here yet — add your first completed project or achievement." />
        : <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {items.map(item => (
            <Panel key={item.id}>
              <div className="mb-2 flex items-start justify-between gap-2">
                <h3 className="font-display font-bold text-ink">{item.title}</h3>
                <span className={`tag ${item.tag === 'CERTIFICATION' ? 'tag-teal' : item.tag === 'ACHIEVEMENT' ? 'tag-orange' : ''}`}>
                  {item.tag.toLowerCase()}
                </span>
              </div>
              <p className="mb-3 line-clamp-3 text-sm text-muted">{item.description}</p>
              <div className="flex items-center justify-between text-xs text-muted">
                <span>{item.projectTitle ? `🔗 ${item.projectTitle}` : (item.link ? '🌐 external link' : 'standalone')}</span>
                <StatusBadge value={item.visibility} />
              </div>
              <div className="mt-3 flex gap-2">
                <button className="btn-ghost btn-sm" onClick={() => setEditing(item)}>Edit</button>
                <button className="btn-danger btn-sm" onClick={() => remove(item.id)}>Delete</button>
              </div>
            </Panel>
          ))}
        </div>}
    </>
  )
}
