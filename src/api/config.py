"""API Configuration - Environment-based settings."""

import os
from functools import lru_cache
from typing import List

from pydantic import ConfigDict, Field, field_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Database
    database_url: str = Field(
        default="postgresql+asyncpg://tender_user:tender_pass@localhost:5432/tender_tracker",
        description="PostgreSQL connection URL",
    )

    @field_validator("database_url", mode="before")
    @classmethod
    def fix_database_url(cls, v: str) -> str:
        """Convert Railway's postgres:// to postgresql+asyncpg:// format."""
        if v and v.startswith("postgres://"):
            v = v.replace("postgres://", "postgresql+asyncpg://", 1)
        elif v and v.startswith("postgresql://"):
            v = v.replace("postgresql://", "postgresql+asyncpg://", 1)
        return v

    # JWT Authentication
    jwt_secret_key: str = Field(
        default="change-me-in-production",
        description="Secret key for JWT token signing",
    )
    jwt_algorithm: str = Field(default="HS256", description="JWT signing algorithm")
    jwt_expire_minutes: int = Field(
        default=1440, description="JWT token expiration in minutes (24 hours)"
    )

    # CORS
    cors_origins: str = Field(
        default="http://localhost:3000,http://localhost:5173",
        description="Comma-separated list of allowed origins",
    )

    # API Server
    api_host: str = Field(default="0.0.0.0", description="API server host")
    api_port: int = Field(default=8000, description="API server port")
    debug: bool = Field(default=False, description="Enable debug mode")

    @field_validator("debug", mode="before")
    @classmethod
    def strip_debug(cls, v: object) -> object:
        """Strip whitespace from debug value (Railway UI sometimes adds trailing space)."""
        if isinstance(v, str):
            return v.strip()
        return v

    # Application
    app_name: str = Field(default="Tender Tracker Pro", description="Application name")
    app_version: str = Field(default="1.0.3", description="Application version")

    @property
    def cors_origins_list(self) -> List[str]:
        """Parse CORS origins string into list."""
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]

    model_config = ConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
