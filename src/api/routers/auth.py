"""Authentication router - register, login, token management."""

from fastapi import APIRouter, HTTPException, status

from src.api.dependencies import CurrentUser, DbSession
from src.api.schemas import PasswordChange, Token, UserCreate, UserLogin, UserResponse, UserUpdate
from src.api.services.auth_service import (
    authenticate_user,
    change_password,
    create_access_token,
    create_user,
    get_user_by_email,
    update_user,
)

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=Token, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserCreate, db: DbSession) -> Token:
    """Register a new user account."""
    # Check if email already exists
    existing_user = await get_user_by_email(db, user_data.email)
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered",
        )

    # Create new user
    user = await create_user(
        db=db,
        email=user_data.email,
        password=user_data.password,
        full_name=user_data.full_name,
        company_name=user_data.company_name,
    )

    # Generate token
    access_token = create_access_token(user.id, user.email)

    return Token(access_token=access_token)


@router.post("/login", response_model=Token)
async def login(credentials: UserLogin, db: DbSession) -> Token:
    """Authenticate and get JWT token."""
    user = await authenticate_user(db, credentials.email, credentials.password)

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    access_token = create_access_token(user.id, user.email)

    return Token(access_token=access_token)


@router.post("/refresh", response_model=Token)
async def refresh_token(current_user: CurrentUser) -> Token:
    """Refresh access token."""
    access_token = create_access_token(current_user.id, current_user.email)
    return Token(access_token=access_token)


@router.get("/me", response_model=UserResponse)
async def get_current_user_info(current_user: CurrentUser) -> UserResponse:
    """Get current authenticated user info."""
    return UserResponse.model_validate(current_user)


@router.put("/me", response_model=UserResponse)
async def update_profile(
    profile_data: UserUpdate,
    current_user: CurrentUser,
    db: DbSession,
) -> UserResponse:
    """Update current user's profile."""
    updated_user = await update_user(
        db=db,
        user=current_user,
        full_name=profile_data.full_name,
        company_name=profile_data.company_name,
    )
    await db.commit()
    return UserResponse.model_validate(updated_user)


@router.post("/change-password")
async def change_user_password(
    password_data: PasswordChange,
    current_user: CurrentUser,
    db: DbSession,
) -> dict:
    """Change current user's password."""
    success = await change_password(
        db=db,
        user=current_user,
        current_password=password_data.current_password,
        new_password=password_data.new_password,
    )

    if not success:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Current password is incorrect",
        )

    await db.commit()
    return {"message": "Password changed successfully"}
