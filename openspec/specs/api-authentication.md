# Authentication Specification

## Token Type
JWT (JSON Web Token)

## Token Payload
```json
{
  "sub": "<user_id>",
  "email": "<user_email>",
  "exp": "<expiration_timestamp>",
  "iat": "<issued_at_timestamp>"
}
```

## Token Lifetime
- Access token: 24 hours
- Refresh token: 7 days (future enhancement)

## Password Requirements
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit

## Security
- Passwords hashed with bcrypt (12 rounds)
- Tokens signed with HS256 algorithm
- Secret key from environment variable
