# Docker Compose manual test checklist

Run from the repository root after the servers are up. These tests create a product and an order in the persistent local databases. They do not delete existing data. Default host ports are shown; change the URLs if `INVENTORY_HTTP_PORT` or `ORDER_HTTP_PORT` was overridden. Record failures, responses and relevant logs.

## 1. Startup

- [ ] Run `docker compose ps`: both databases should be healthy and both application containers running. `running` alone does not establish application readiness.
- [ ] Run `docker compose logs --tail=100 inventory order`: check that both applications started and Flyway completed without database connection or migration errors. Allow time for startup: Order does not wait for Inventory readiness.

## 2. GraphQL and cross-service workflow

Use PowerShell. The helper returns the full GraphQL response; inspect `errors` even when HTTP succeeds. Run the blocks in order in the same session.

```powershell
$inventoryUrl = 'http://localhost:8080/graphql'
$orderUrl = 'http://localhost:8081/graphql'
function Invoke-GraphQL($url, $query, $variables = @{}) {
    $body = @{ query = $query; variables = $variables } | ConvertTo-Json -Depth 20
    Invoke-RestMethod -Uri $url -Method Post -ContentType 'application/json' -Body $body
}
```

- [ ] Create an active product with stock 10 and price 4.75. Expect an ID and the supplied values, with no `errors`. Save the ID for subsequent checks.

```powershell
$productResponse = Invoke-GraphQL $inventoryUrl 'mutation($input: CreateProductInput!) { createProduct(input: $input) { id name quantityInStock unit pricePerUnit status } }' @{ input = @{ name = "Docker test $([guid]::NewGuid().ToString().Substring(0,8))"; quantityInStock = 10; unit = 'UNITS'; pricePerUnit = 4.75; status = 'ACTIVE' } }
$productResponse | ConvertTo-Json -Depth 20
$productId = $productResponse.data.createProduct.id
```

- [ ] Read the product back. Expect the same ID, stock and price, confirming Inventory can write and read its database.

```powershell
Invoke-GraphQL $inventoryUrl 'query($input: GetProductByIdInput!) { productById(input: $input) { id name quantityInStock pricePerUnit } }' @{ input = @{ id = $productId } } | ConvertTo-Json -Depth 20
```

- [ ] Create an order for two units. Expect `WAITING_PAYMENT`, one item with the product ID, quantity 2, unit price 4.75 and total 9.50, with no `errors`. This exercises Order-to-Inventory gRPC and the Order database.

```powershell
$orderResponse = Invoke-GraphQL $orderUrl 'mutation($input: CreateOrderInput!) { createOrder(input: $input) { id status totalAmount items { productId quantity unitPrice totalAmount } } }' @{ input = @{ items = @(@{ productId = $productId; quantity = 2 }) } }
$orderResponse | ConvertTo-Json -Depth 20
$orderId = $orderResponse.data.createOrder.id
```

- [ ] Read the order back by ID; expect the same item and total.

```powershell
Invoke-GraphQL $orderUrl 'query($input: OrderIdInput!) { orderById(input: $input) { id status totalAmount items { productId quantity unitPrice totalAmount } } }' @{ input = @{ id = $orderId } } | ConvertTo-Json -Depth 20
```

- [ ] Request an order for 11 units. Expect a GraphQL error and no successfully created order. Re-read the product and expect stock still 10; order creation checks availability but does not itself debit stock.

```powershell
Invoke-GraphQL $orderUrl 'mutation($input: CreateOrderInput!) { createOrder(input: $input) { id } }' @{ input = @{ items = @(@{ productId = $productId; quantity = 11 }) } } | ConvertTo-Json -Depth 20
Invoke-GraphQL $inventoryUrl 'query($input: ProductIdInput!) { availableStock(input: $input) }' @{ input = @{ id = $productId } } | ConvertTo-Json -Depth 20
```

- [ ] Try decreasing stock by 11. Expect a GraphQL error with `extensions.code` = `CONFLICT`; re-read stock and expect 10.

```powershell
Invoke-GraphQL $inventoryUrl 'mutation($input: ChangeStockInput!) { decreaseStock(input: $input) { id quantityInStock } }' @{ input = @{ id = $productId; quantity = 11 } } | ConvertTo-Json -Depth 20
Invoke-GraphQL $inventoryUrl 'query($input: ProductIdInput!) { availableStock(input: $input) }' @{ input = @{ id = $productId } } | ConvertTo-Json -Depth 20
```

## 3. Persistence across container recreation

- [ ] Run `docker compose down` followed by `docker compose up -d`. This briefly stops both services; do not add `-v`, which deletes the database volumes. Wait for startup as above.
- [ ] Repeat the product and order read queries from section 2 in the same PowerShell session. Both IDs and their values should still be present. If the session was closed, substitute the recorded IDs.
- [ ] Check `docker compose ps` and recent application logs again for healthy databases and successful application startup.
