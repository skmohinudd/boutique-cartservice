# Boutique Cart Service

Owns temporary shopping-cart state.

## Dependencies

- User Service
- Product Catalog Service
- Inventory Service
- Redis

## Endpoints

- `POST /api/v1/carts/{userId}/items`
- `GET /api/v1/carts/{userId}`
- `PUT /api/v1/carts/{userId}/items/{productId}`
- `DELETE /api/v1/carts/{userId}/items/{productId}`
- `DELETE /api/v1/carts/{userId}`

Cart entries expire after 30 days by default.
