# Frontend Development Roadmap

**Created**: 2026-01-12
**Current Status**: Backend Complete, Frontend Not Started
**Goal**: Build MVP web application for tender management

---

## 🎯 MVP ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────┐
│                       WEB BROWSER                            │
│                                                              │
│  React Frontend (Tailwind CSS)                              │
│  ├── Dashboard                                              │
│  ├── Tender List                                            │
│  ├── Tender Detail                                          │
│  └── User Settings                                          │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP/REST
┌─────────────────────▼───────────────────────────────────────┐
│                    FastAPI Server                            │
│                                                              │
│  REST API Endpoints                                         │
│  ├── GET  /api/tenders          (list with filters)        │
│  ├── GET  /api/tenders/{id}     (details)                  │
│  ├── POST /api/tenders/{id}/score                          │
│  ├── POST /api/scrape/{source}  (trigger)                  │
│  └── POST /api/auth/login                                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   PostgreSQL Database                        │
│                                                              │
│  Tables: tenders, scores, users, sources                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│               Background Worker (Optional)                   │
│                                                              │
│  APScheduler: Scheduled scraping every 4 hours              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📅 PHASE 1: DATABASE (Day 1 Morning - 4 hours)

### **Task 1.1: Database Schema Design** (1 hour)
```sql
-- File: migrations/001_initial_schema.sql

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_criteria (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    criteria_json JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sources (
    id SERIAL PRIMARY KEY,
    source_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    enabled BOOLEAN DEFAULT true,
    last_scraped_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tenders (
    id SERIAL PRIMARY KEY,
    source_id INTEGER REFERENCES sources(id),
    
    -- Extracted fields
    title TEXT NOT NULL,
    reference VARCHAR(100) NOT NULL,
    deadline DATE,
    estimated_value DECIMAL(15,2),
    requirements_summary TEXT,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    
    -- File paths
    pdf_path TEXT NOT NULL,
    text_path TEXT,
    
    -- Metadata
    confidence FLOAT,
    extraction_method VARCHAR(50),
    page_count INTEGER,
    text_length INTEGER,
    
    -- Timestamps
    discovered_at TIMESTAMP DEFAULT NOW(),
    extracted_at TIMESTAMP,
    
    UNIQUE(source_id, reference)
);

CREATE TABLE scores (
    id SERIAL PRIMARY KEY,
    tender_id INTEGER REFERENCES tenders(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    explanation TEXT,
    relevance_category VARCHAR(20),
    
    scored_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(tender_id, user_id)
);

CREATE INDEX idx_tenders_deadline ON tenders(deadline);
CREATE INDEX idx_tenders_source ON tenders(source_id);
CREATE INDEX idx_scores_tender ON scores(tender_id);
CREATE INDEX idx_scores_user ON scores(user_id);
CREATE INDEX idx_scores_score ON scores(score);
```

### **Task 1.2: SQLAlchemy Models** (2 hours)
```python
# File: src/db/models.py

from sqlalchemy import Column, Integer, String, Text, Float, DateTime, Boolean, ForeignKey, DECIMAL, Date
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from datetime import datetime

Base = declarative_base()

class User(Base):
    __tablename__ = 'users'
    
    id = Column(Integer, primary_key=True)
    email = Column(String(255), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    company_name = Column(String(255))
    created_at = Column(DateTime, default=datetime.utcnow)
    
    criteria = relationship("UserCriteria", back_populates="user")
    scores = relationship("Score", back_populates="user")

class UserCriteria(Base):
    __tablename__ = 'user_criteria'
    
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey('users.id', ondelete='CASCADE'))
    criteria_json = Column(JSONB, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User", back_populates="criteria")

class Source(Base):
    __tablename__ = 'sources'
    
    id = Column(Integer, primary_key=True)
    source_id = Column(String(50), unique=True, nullable=False)
    name = Column(String(255), nullable=False)
    url = Column(Text, nullable=False)
    enabled = Column(Boolean, default=True)
    last_scraped_at = Column(DateTime)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    tenders = relationship("Tender", back_populates="source")

class Tender(Base):
    __tablename__ = 'tenders'
    
    id = Column(Integer, primary_key=True)
    source_id = Column(Integer, ForeignKey('sources.id'))
    
    # Extracted fields
    title = Column(Text, nullable=False)
    reference = Column(String(100), nullable=False)
    deadline = Column(Date)
    estimated_value = Column(DECIMAL(15, 2))
    requirements_summary = Column(Text)
    contact_name = Column(String(255))
    contact_email = Column(String(255))
    
    # File paths
    pdf_path = Column(Text, nullable=False)
    text_path = Column(Text)
    
    # Metadata
    confidence = Column(Float)
    extraction_method = Column(String(50))
    page_count = Column(Integer)
    text_length = Column(Integer)
    
    # Timestamps
    discovered_at = Column(DateTime, default=datetime.utcnow)
    extracted_at = Column(DateTime)
    
    source = relationship("Source", back_populates="tenders")
    scores = relationship("Score", back_populates="tender")

class Score(Base):
    __tablename__ = 'scores'
    
    id = Column(Integer, primary_key=True)
    tender_id = Column(Integer, ForeignKey('tenders.id', ondelete='CASCADE'))
    user_id = Column(Integer, ForeignKey('users.id', ondelete='CASCADE'))
    
    score = Column(Integer, nullable=False)
    explanation = Column(Text)
    relevance_category = Column(String(20))
    
    scored_at = Column(DateTime, default=datetime.utcnow)
    
    tender = relationship("Tender", back_populates="scores")
    user = relationship("User", back_populates="scores")
```

### **Task 1.3: Database Connection** (1 hour)
```python
# File: src/db/database.py

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from contextlib import contextmanager
import os

DATABASE_URL = os.getenv(
    "DATABASE_URL", 
    "postgresql://postgres:password@localhost/tender_tracker"
)

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)

def init_db():
    """Initialize database tables."""
    from .models import Base
    Base.metadata.create_all(bind=engine)

@contextmanager
def get_db():
    """Get database session context manager."""
    db = SessionLocal()
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
```

---

## 📅 PHASE 2: REST API (Day 1 Afternoon - 8 hours)

### **Task 2.1: FastAPI Setup** (1 hour)
```python
# File: src/api/main.py

from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session

from db.database import get_db, init_db
from api.routers import tenders, auth, scraping

app = FastAPI(
    title="Tender Tracker API",
    version="1.0.0",
    description="API for automated tender discovery and scoring"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # React dev server
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router, prefix="/api/auth", tags=["auth"])
app.include_router(tenders.router, prefix="/api/tenders", tags=["tenders"])
app.include_router(scraping.router, prefix="/api/scraping", tags=["scraping"])

@app.on_event("startup")
async def startup():
    init_db()

@app.get("/")
async def root():
    return {"message": "Tender Tracker API", "version": "1.0.0"}

@app.get("/health")
async def health():
    return {"status": "healthy"}
```

### **Task 2.2: Authentication Endpoints** (2 hours)
```python
# File: src/api/routers/auth.py

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from passlib.context import CryptContext
from jose import jwt, JWTError
from datetime import datetime, timedelta
from sqlalchemy.orm import Session

from db.database import get_db
from db.models import User
from api.schemas import UserCreate, UserResponse, Token

router = APIRouter()
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")

SECRET_KEY = "your-secret-key-here"  # Move to env
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24  # 24 hours

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: int = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=401, detail="User not found")
    return user

@router.post("/register", response_model=UserResponse)
async def register(user_data: UserCreate, db: Session = Depends(get_db)):
    # Check if user exists
    existing = db.query(User).filter(User.email == user_data.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Create user
    user = User(
        email=user_data.email,
        password_hash=pwd_context.hash(user_data.password),
        company_name=user_data.company_name
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    
    return user

@router.post("/login", response_model=Token)
async def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(User.email == form_data.username).first()
    if not user or not pwd_context.verify(form_data.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    access_token = create_access_token(data={"sub": user.id})
    return {"access_token": access_token, "token_type": "bearer"}
```

### **Task 2.3: Tender Endpoints** (3 hours)
```python
# File: src/api/routers/tenders.py

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import date

from db.database import get_db
from db.models import Tender, Score
from api.routers.auth import get_current_user
from api.schemas import TenderResponse, TenderDetail, ScoreRequest, ScoreResponse

router = APIRouter()

@router.get("/", response_model=List[TenderResponse])
async def list_tenders(
    skip: int = Query(0, ge=0),
    limit: int = Query(50, le=100),
    search: Optional[str] = None,
    deadline_from: Optional[date] = None,
    deadline_to: Optional[date] = None,
    min_score: Optional[int] = None,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    query = db.query(Tender)
    
    # Apply filters
    if search:
        query = query.filter(
            Tender.title.ilike(f"%{search}%") |
            Tender.reference.ilike(f"%{search}%")
        )
    
    if deadline_from:
        query = query.filter(Tender.deadline >= deadline_from)
    
    if deadline_to:
        query = query.filter(Tender.deadline <= deadline_to)
    
    # Get tenders with scores
    tenders = query.offset(skip).limit(limit).all()
    
    # Add user scores
    result = []
    for tender in tenders:
        score = db.query(Score).filter(
            Score.tender_id == tender.id,
            Score.user_id == current_user.id
        ).first()
        
        if min_score and (not score or score.score < min_score):
            continue
        
        result.append({
            **tender.__dict__,
            "score": score.score if score else None,
            "relevance_category": score.relevance_category if score else None
        })
    
    return result

@router.get("/{tender_id}", response_model=TenderDetail)
async def get_tender(
    tender_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    tender = db.query(Tender).filter(Tender.id == tender_id).first()
    if not tender:
        raise HTTPException(status_code=404, detail="Tender not found")
    
    # Get score for this user
    score = db.query(Score).filter(
        Score.tender_id == tender_id,
        Score.user_id == current_user.id
    ).first()
    
    return {
        **tender.__dict__,
        "score": score.score if score else None,
        "explanation": score.explanation if score else None,
        "relevance_category": score.relevance_category if score else None
    }

@router.post("/{tender_id}/score", response_model=ScoreResponse)
async def score_tender(
    tender_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Calculate relevance score for tender (triggers LLM)."""
    tender = db.query(Tender).filter(Tender.id == tender_id).first()
    if not tender:
        raise HTTPException(status_code=404, detail="Tender not found")
    
    # TODO: Call LLM scoring logic here
    # from scoring import RelevanceScorer
    # score_result = await scorer.score_single(tender_info)
    
    # For now, placeholder
    score_value = 75  # Placeholder
    
    # Upsert score
    score = db.query(Score).filter(
        Score.tender_id == tender_id,
        Score.user_id == current_user.id
    ).first()
    
    if score:
        score.score = score_value
        score.explanation = "Placeholder explanation"
        score.relevance_category = "Good"
    else:
        score = Score(
            tender_id=tender_id,
            user_id=current_user.id,
            score=score_value,
            explanation="Placeholder explanation",
            relevance_category="Good"
        )
        db.add(score)
    
    db.commit()
    db.refresh(score)
    
    return score
```

### **Task 2.4: Scraping Endpoints** (2 hours)
```python
# File: src/api/routers/scraping.py

from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session

from db.database import get_db
from db.models import Source
from api.routers.auth import get_current_user
from api.schemas import ScrapeRequest, ScrapeResponse

router = APIRouter()

async def run_scraping_task(source_id: str):
    """Background task to run scraping."""
    # TODO: Import and call scraping logic
    # from navigator import BrowserEngine, PDFDiscovery, PDFDownloader
    # from extractor import PDFExtractor
    # Run the pipeline...
    pass

@router.post("/scrape/{source_id}", response_model=ScrapeResponse)
async def trigger_scraping(
    source_id: str,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Trigger scraping for a source."""
    source = db.query(Source).filter(Source.source_id == source_id).first()
    if not source:
        raise HTTPException(status_code=404, detail="Source not found")
    
    if not source.enabled:
        raise HTTPException(status_code=400, detail="Source is disabled")
    
    # Add background task
    background_tasks.add_task(run_scraping_task, source_id)
    
    return {
        "message": f"Scraping started for {source.name}",
        "source_id": source_id,
        "status": "started"
    }

@router.get("/sources")
async def list_sources(
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """List all configured sources."""
    sources = db.query(Source).all()
    return sources
```

---

## 📅 PHASE 3: WEB FRONTEND (Day 2-3 - 12 hours)

### **Task 3.1: React Project Setup** (1 hour)
```bash
# Create React app
npx create-react-app frontend
cd frontend

# Install dependencies
npm install axios react-router-dom @tanstack/react-query
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Configure Tailwind (tailwind.config.js)
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: { extend: {} },
  plugins: [],
}
```

### **Task 3.2: Authentication Pages** (2 hours)
```jsx
// src/pages/Login.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const formData = new FormData();
      formData.append('username', email);
      formData.append('password', password);

      const response = await axios.post('/api/auth/login', formData);
      localStorage.setItem('token', response.data.access_token);
      navigate('/dashboard');
    } catch (error) {
      alert('Login failed');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="text-3xl font-bold text-center">
            Tender Tracker Pro
          </h2>
        </div>
        <form onSubmit={handleLogin} className="mt-8 space-y-6">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Email"
            className="w-full px-3 py-2 border rounded"
            required
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Password"
            className="w-full px-3 py-2 border rounded"
            required
          />
          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
          >
            Login
          </button>
        </form>
      </div>
    </div>
  );
}
```

### **Task 3.3: Dashboard** (3 hours)
```jsx
// src/pages/Dashboard.jsx
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

function Dashboard() {
  const { data: stats } = useQuery(['stats'], async () => {
    const token = localStorage.getItem('token');
    const res = await axios.get('/api/tenders/stats', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.data;
  });

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-8">Dashboard</h1>
      
      <div className="grid grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="text-2xl font-bold">{stats?.total || 0}</div>
          <div className="text-gray-600">Total Tenders</div>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="text-2xl font-bold">{stats?.high_score || 0}</div>
          <div className="text-gray-600">High Priority</div>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="text-2xl font-bold">
            R{(stats?.avg_value || 0).toLocaleString()}
          </div>
          <div className="text-gray-600">Avg Value</div>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="text-2xl font-bold">{stats?.success_rate || 0}%</div>
          <div className="text-gray-600">Success Rate</div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-bold mb-4">Recent High-Priority Tenders</h2>
        {/* Tender list component */}
      </div>
    </div>
  );
}
```

### **Task 3.4: Tender List** (3 hours)
```jsx
// src/pages/TenderList.jsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Link } from 'react-router-dom';

function TenderList() {
  const [search, setSearch] = useState('');
  const [minScore, setMinScore] = useState(50);

  const { data: tenders, isLoading } = useQuery(['tenders', search, minScore], async () => {
    const token = localStorage.getItem('token');
    const res = await axios.get('/api/tenders', {
      headers: { Authorization: `Bearer ${token}` },
      params: { search, min_score: minScore }
    });
    return res.data;
  });

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-8">Tenders</h1>

      <div className="flex gap-4 mb-6">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search tenders..."
          className="flex-1 px-4 py-2 border rounded"
        />
        <select
          value={minScore}
          onChange={(e) => setMinScore(Number(e.target.value))}
          className="px-4 py-2 border rounded"
        >
          <option value={0}>All Scores</option>
          <option value={50}>Score ≥ 50</option>
          <option value={70}>Score ≥ 70</option>
          <option value={90}>Score ≥ 90</option>
        </select>
      </div>

      {isLoading ? (
        <div>Loading...</div>
      ) : (
        <div className="space-y-4">
          {tenders?.map(tender => (
            <Link
              key={tender.id}
              to={`/tenders/${tender.id}`}
              className="block bg-white p-6 rounded-lg shadow hover:shadow-lg transition"
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <h3 className="text-xl font-bold mb-2">{tender.title}</h3>
                  <p className="text-gray-600 mb-2">
                    Ref: {tender.reference}
                  </p>
                  <p className="text-sm text-gray-500">
                    Deadline: {tender.deadline || 'N/A'} | 
                    Value: R{tender.estimated_value?.toLocaleString() || 'N/A'}
                  </p>
                </div>
                {tender.score && (
                  <div className="ml-4 text-right">
                    <div className="text-3xl font-bold text-blue-600">
                      {tender.score}
                    </div>
                    <div className="text-sm text-gray-600">
                      {tender.relevance_category}
                    </div>
                  </div>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
```

### **Task 3.5: Tender Detail** (2 hours)
```jsx
// src/pages/TenderDetail.jsx
import { useParams } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import axios from 'axios';

function TenderDetail() {
  const { id } = useParams();
  const token = localStorage.getItem('token');

  const { data: tender } = useQuery(['tender', id], async () => {
    const res = await axios.get(`/api/tenders/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.data;
  });

  const scoreMutation = useMutation(async () => {
    await axios.post(`/api/tenders/${id}/score`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    });
  });

  if (!tender) return <div>Loading...</div>;

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <div className="bg-white rounded-lg shadow p-8">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-3xl font-bold mb-2">{tender.title}</h1>
            <p className="text-gray-600">Reference: {tender.reference}</p>
          </div>
          {tender.score ? (
            <div className="text-center">
              <div className="text-5xl font-bold text-blue-600">
                {tender.score}
              </div>
              <div className="text-sm text-gray-600">
                {tender.relevance_category}
              </div>
            </div>
          ) : (
            <button
              onClick={() => scoreMutation.mutate()}
              className="px-4 py-2 bg-blue-600 text-white rounded"
            >
              Calculate Score
            </button>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4 mb-6">
          <div>
            <div className="font-bold text-gray-700">Deadline</div>
            <div>{tender.deadline || 'Not specified'}</div>
          </div>
          <div>
            <div className="font-bold text-gray-700">Estimated Value</div>
            <div>
              {tender.estimated_value 
                ? `R${tender.estimated_value.toLocaleString()}` 
                : 'Not specified'}
            </div>
          </div>
          <div>
            <div className="font-bold text-gray-700">Contact</div>
            <div>{tender.contact_name || 'N/A'}</div>
            <div className="text-sm text-gray-600">
              {tender.contact_email || ''}
            </div>
          </div>
        </div>

        {tender.explanation && (
          <div className="mb-6 p-4 bg-blue-50 rounded">
            <div className="font-bold text-gray-700 mb-2">
              Score Explanation
            </div>
            <div>{tender.explanation}</div>
          </div>
        )}

        {tender.requirements_summary && (
          <div>
            <div className="font-bold text-gray-700 mb-2">
              Requirements Summary
            </div>
            <div className="text-gray-600">
              {tender.requirements_summary}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

### **Task 3.6: App Routing** (1 hour)
```jsx
// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import TenderList from './pages/TenderList';
import TenderDetail from './pages/TenderDetail';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/tenders" element={<TenderList />} />
          <Route path="/tenders/:id" element={<TenderDetail />} />
          <Route path="/" element={<Navigate to="/dashboard" />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
```

---

## 📅 PHASE 4: DEPLOYMENT (Day 3 - 2 hours)

### **Task 4.1: Docker Setup**
```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY src/ ./src/
COPY config/ ./config/
COPY prompts/ ./prompts/

CMD ["uvicorn", "src.api.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### **Task 4.2: Docker Compose**
```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: tender_tracker
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  api:
    build: .
    ports:
      - "8000:8000"
    depends_on:
      - postgres
    environment:
      DATABASE_URL: postgresql://postgres:password@postgres/tender_tracker
    volumes:
      - ./models:/app/models
      - ./documents:/app/documents

  frontend:
    image: node:18
    working_dir: /app
    command: npm start
    ports:
      - "3000:3000"
    volumes:
      - ./frontend:/app

volumes:
  postgres_data:
```

---

## 📝 SUMMARY CHECKLIST

### **Day 1 Morning: Database** (4 hours)
- [ ] Design database schema
- [ ] Create SQLAlchemy models
- [ ] Setup database connection
- [ ] Test migrations

### **Day 1 Afternoon: API** (8 hours)
- [ ] Setup FastAPI project
- [ ] Implement authentication (JWT)
- [ ] Create tender endpoints
- [ ] Create scraping endpoints
- [ ] Test API with Postman

### **Day 2-3: Frontend** (12 hours)
- [ ] Setup React project
- [ ] Create login page
- [ ] Create dashboard
- [ ] Create tender list
- [ ] Create tender detail
- [ ] Setup routing
- [ ] Test end-to-end

### **Day 3: Deployment** (2 hours)
- [ ] Create Dockerfile
- [ ] Create docker-compose.yml
- [ ] Test deployment locally
- [ ] Document deployment steps

---

## 🎯 TOTAL EFFORT

**Estimated Time**: 2-3 days (26 hours)
**Complexity**: Medium
**Tech Stack**:
- Backend: FastAPI, SQLAlchemy, PostgreSQL
- Frontend: React, TailwindCSS, React Query
- Deployment: Docker, Docker Compose

---

**End of Roadmap**
**Created**: 2026-01-12
**Next Action**: Start with Phase 1 (Database) when ready
