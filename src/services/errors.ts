export class SalesbookError extends Error {
  constructor(message: string) {
    super(message)
    this.name = new.target.name
  }
}

export class CustomerNotFoundError extends SalesbookError {
  constructor() {
    super('Cliente não encontrado.')
  }
}

export class ProductNotFoundError extends SalesbookError {
  constructor() {
    super('Produto não encontrado.')
  }
}

export class OrderNotFoundError extends SalesbookError {
  constructor() {
    super('Pedido não encontrado.')
  }
}

export class OrderItemNotFoundError extends SalesbookError {
  constructor() {
    super('Item do pedido não encontrado.')
  }
}

export class PaymentNotFoundError extends SalesbookError {
  constructor() {
    super('Pagamento não encontrado.')
  }
}

export class InvalidQuantityError extends SalesbookError {
  constructor() {
    super('A quantidade deve ser um número inteiro maior ou igual a 1.')
  }
}

export class InsufficientStockError extends SalesbookError {
  constructor(productName: string) {
    super(`Estoque insuficiente para "${productName}".`)
  }
}

export class DuplicateNameError extends SalesbookError {
  constructor(entity: 'cliente' | 'produto', name: string) {
    super(`Já existe um ${entity} com o nome "${name}".`)
  }
}

export class InvalidNameError extends SalesbookError {
  constructor() {
    super('Informe um nome válido.')
  }
}

export class InvalidPriceError extends SalesbookError {
  constructor() {
    super('O preço deve ser um número maior ou igual a zero.')
  }
}

export class InvalidPaymentError extends SalesbookError {
  constructor() {
    super('O valor do pagamento deve ser maior que zero.')
  }
}

export class InvalidStockError extends SalesbookError {
  constructor() {
    super('A quantidade em estoque deve ser um número inteiro maior ou igual a zero.')
  }
}

export class ReferencedEntityError extends SalesbookError {
  constructor(entity: 'cliente' | 'produto', reason: string) {
    super(`Não é possível excluir este ${entity}: ${reason}`)
  }
}
