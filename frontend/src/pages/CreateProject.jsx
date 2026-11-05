import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { PageHeading, Field, useToast } from '../components'
import { get, post, put } from '../api'

/** U3 / F4 — create a project as a draft or publish it right away; also used to edit (owner only). */
export default function CreateProject({ editMode }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const editId = params.get('id')
  const toast = useToast()
  const [skills, setSkills] = useState([])
  const [form, setForm] = useState({
    title: '', description: '', goals: '', category: 'Web Development',
    teamSizeMax: 5, startDate: '', targetDeadline: '',
  })
  const [picked, setPicked] = useState([]) // { skillId, requiredLevel }
  const [roles, setRoles] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    get('/api/skills').then(setSkills).catch(() => {})
    if (editMode && editId) {
      get(`/api/projects/${editId}`).then((d) => {
        setForm({
          title: d.summary.title, description: d.summary.description || '', goals: d.goals || '',
          category: d.summary.category || 'Web Development',
          teamSizeMax: d.summary.teamSizeMax,
          startDate: d.summary.startDate || '', targetDeadline: d.summary.targetDeadline || '',
        })
        setPicked((d.skills || []).map(s => ({ skillId: s.skillId, requiredLevel: s.requiredLevel || 'BEGINNER' })))
      }).catch((ex) => setError(ex.message))
    }
  }, [editMode, editId])

  const toggleSkill = (id) => setPicked((list) => list.some(x => x.skillId === id)
    ? list.filter(x => x.skillId !== id)
    : [...list, { skillId: id, requiredLevel: 'BEGINNER' }])

  const setLevel = (id, level) => setPicked((list) => list.map(x => x.skillId === id ? { ...x, requiredLevel: level } : x))

  const save = async (status) => {
    setBusy(true)
    setError('')
    const body = { ...form, status, teamSizeMax: Number(form.teamSizeMax), skills: picked, roles }
    try {
      let project = editMode && editId
        ? await put(`/api/projects/${editId}`, body)
        : await post('/api/projects', body)
      if (!editMode && status === 'publish-draft-later') {
        // handled below by explicit buttons
      }
      if (!editMode && status === 'PUBLISHED') {
        await post(`/api/projects/${project.id}/publish`, {}).catch(() => {})
      }
      toast(editMode ? 'Project updated ✅' : status === 'PUBLISHED' ? 'Project published 📣' : 'Draft saved 📝')
      navigate(`/projects/${project.id}`)
    } catch (ex) {
      setError(ex.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeading title={editMode ? 'Edit project' : 'Create a project'}
        lead="Describe the goal, the skills you need and the roles to fill — U3 / F4." />
      <div className="grid gap-5 lg:grid-cols-[1.6fr_1fr]">
        <div className="panel">
          <Field label="Project title">
            <input className="field-input" required value={form.title}
                   onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Campus Event Hub" />
          </Field>
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Category">
              <select className="field-input" value={form.category}
                      onChange={(e) => setForm({ ...form, category: e.target.value })}>
                <option>Web Development</option><option>Backend</option><option>Data & AI</option>
                <option>Mobile</option><option>Design</option><option>Learning</option>
              </select>
            </Field>
            <Field label="Max team size">
              <input className="field-input" type="number" min="1" max="20" value={form.teamSizeMax}
                     onChange={(e) => setForm({ ...form, teamSizeMax: e.target.value })} />
            </Field>
            <Field label="Start date">
              <input className="field-input" type="date" value={form.startDate}
                     onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            </Field>
            <Field label="Target deadline">
              <input className="field-input" type="date" value={form.targetDeadline}
                     onChange={(e) => setForm({ ...form, targetDeadline: e.target.value })} />
            </Field>
          </div>
          <Field label="Description">
            <textarea className="field-input" rows={4} value={form.description}
                      onChange={(e) => setForm({ ...form, description: e.target.value })}
                      placeholder="What will you build, and why is it exciting?" />
          </Field>
          <Field label="Goals & scope" hint="Visible to approved members; the description stays public.">
            <textarea className="field-input" rows={3} value={form.goals}
                      onChange={(e) => setForm({ ...form, goals: e.target.value })}
                      placeholder="Shipping, demo plan, deliverables…" />
          </Field>

          <Field label="Roles to fill (one per line)">
            <textarea className="field-input" rows={2} value={roles.join('\n')}
                      onChange={(e) => setRoles(e.target.value.split('\n'))}
                      placeholder={'Frontend Developer\nDesigner'} />
          </Field>
          {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
          <div className="flex flex-wrap gap-2">
            {!editMode && <button className="btn-secondary" disabled={busy} onClick={() => save('DRAFT')}>Save draft</button>}
            <button className="btn-primary" disabled={busy} onClick={() => save('PUBLISHED')}>
              {editMode ? 'Save changes' : 'Save & publish'}
            </button>
          </div>
        </div>

        <div className="panel">
          <h3 className="panel-title mb-3 text-base">Required skills</h3>
          <p className="mb-3 text-xs text-muted">Pick skills and the level you expect.</p>
          <div className="max-h-[420px] space-y-2 overflow-y-auto pr-1">
            {skills.map(s => {
              const mine = picked.find(x => x.skillId === s.id)
              return (
                <div key={s.id} className={`rounded-xl border p-2.5 text-sm ${mine ? 'border-brand/40 bg-soft' : 'border-line'}`}>
                  <label className="flex items-center gap-2 font-semibold">
                    <input type="checkbox" className="h-4 w-4 accent-[#3659d9]" checked={!!mine}
                           onChange={() => toggleSkill(s.id)} /> {s.name}
                  </label>
                  {mine && (
                    <select className="field-input mt-1.5 py-1.5 text-xs" value={mine.requiredLevel}
                            onChange={(e) => setLevel(s.id, e.target.value)}>
                      <option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option><option>EXPERT</option>
                    </select>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </>
  )
}
