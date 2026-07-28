import { Link } from 'react-router-dom';

function Centered({ icon, code, title, message }: { icon: string; code: string; title: string; message: string }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center text-center" style={{ minHeight: '70vh' }}>
      <i className={`bi bi-${icon}`} style={{ fontSize: '3.5rem', color: 'var(--dp-blue)' }} />
      <h1 className="mt-3 mb-1" style={{ color: 'var(--dp-navy)' }}>{code}</h1>
      <h5 className="text-muted">{title}</h5>
      <p className="text-muted">{message}</p>
      <Link to="/dashboard" className="btn btn-primary mt-2">Back to Dashboard</Link>
    </div>
  );
}

export const ForbiddenPage = () => (
  <Centered icon="shield-lock" code="403" title="Access denied"
    message="Your role does not have permission to view this page." />
);

export const NotFoundPage = () => (
  <Centered icon="compass" code="404" title="Page not found"
    message="The page you are looking for does not exist." />
);
