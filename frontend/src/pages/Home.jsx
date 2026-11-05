import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

// Home page — kept intentionally simple and student-friendly.
// Plain sections, no gradients or fancy effects, matches the rest of the app.
export default function Home() {
  const { user } = useAuth()
  return (
    <>
      {/* simple intro */}
      <section className="mb-10 max-w-2xl">
        <h1 className="text-3xl font-bold text-ink">SkillBridge — a small site for student collaboration</h1>
        <p className="mt-3 text-muted">
          This is a student project: you can list your skills, find teammates for your project,
          and plan work together. Login to try it out.
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          {user
            ? <>
              <Link className="btn-primary" to="/dashboard">Go to dashboard</Link>
              <Link className="btn-secondary" to="/projects">See projects</Link>
            </>
            : <>
              <Link className="btn-primary" to="/register">Create an account</Link>
              <Link className="btn-secondary" to="/login">Login</Link>
            </>}
        </div>
        <p className="mt-3 text-xs text-muted">Built as a college mini-project · Spring Boot + React + MySQL</p>
      </section>

      {/* what you can do — three plain cards */}
      <section className="mb-10">
        <h2 className="text-xl font-bold text-ink">What you can do here</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <article className="panel">
            <div className="mb-2 text-2xl">🔍</div>
            <h3 className="font-bold text-ink">Browse projects</h3>
            <p className="mt-1 text-sm text-muted">Read project ideas posted by other students and join one.</p>
          </article>
          <article className="panel">
            <div className="mb-2 text-2xl">👥</div>
            <h3 className="font-bold text-ink">Find teammates</h3>
            <p className="mt-1 text-sm text-muted">Search students by skill (like Java or Figma) and invite them.</p>
          </article>
          <article className="panel">
            <div className="mb-2 text-2xl">📋</div>
            <h3 className="font-bold text-ink">Plan and finish work</h3>
            <p className="mt-1 text-sm text-muted">Create tasks, track simple statuses and show your portfolio the end.</p>
          </article>
        </div>
      </section>

      {/* small footer-ish note */}
      <section className="panel">
        <h2 className="text-lg font-bold text-ink">Nothing fancy — just useful</h2>
        <p className="mt-1 text-sm text-muted">
          Add your skills in your profile, so teammates searching for "Java" or "React"
          can actually find you. That's how the matching works!
        </p>
      </section>
    </>
  )
}
