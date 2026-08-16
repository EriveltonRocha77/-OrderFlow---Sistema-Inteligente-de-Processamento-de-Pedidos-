# 🛒 OrderFlow - Sistema de Pedidos

Sistema de processamento de pedidos com Design Patterns em Java e Spring Boot.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.5-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📌 O que faz?

Uma API para criar e gerenciar pedidos de uma loja virtual. Suporta produtos digitais e físicos.

**Funcionalidades:**
- Criar pedidos digitais e físicos
- Validar pedidos automaticamente
- Calcular frete
- Notificar cliente por email/SMS
- Atualizar status do pedido

---

## 🎯 Design Patterns Usados

| Padrão | O que faz |
|--------|-----------|
| **Builder** | Cria pedidos de forma flexível |
| **Factory** | Cria diferentes tipos de pedido |
| **Strategy** | Calcula frete de formas diferentes |
| **Chain of Responsibility** | Valida pedido em etapas |
| **Observer** | Envia notificações |
| **Singleton** | Gerencia configurações |

---

## 🛠️ Tecnologias

- Java 17
- Spring Boot 3.1.5
- Spring Data JPA
- H2 Database (banco em memória)
- Maven
- Lombok

---

## 🚀 Como rodar

**1. Clone o projeto**
```bash
git clone https://github.com/seu-usuario/orderflow.git
cd orderflow
2. Rode a aplicação

bash
mvn spring-boot:run
3. Acesse

API: http://localhost:8080

Banco H2: http://localhost:8080/h2-console

📡 Endpoints da API
Criar pedidos
text
POST /api/orders/digital   - Pedido digital
POST /api/orders/physical  - Pedido físico
Gerenciar pedidos
text
GET  /api/orders           - Listar todos
GET  /api/orders/{id}      - Buscar um
PUT  /api/orders/{id}/status?status=PROCESSING - Atualizar status
Status disponíveis
PENDING (Pendente)

PROCESSING (Processando)

SHIPPED (Enviado)

DELIVERED (Entregue)

CANCELLED (Cancelado)

📝 Exemplos
Criar pedido digital
bash
curl -X POST http://localhost:8080/api/orders/digital \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "name": "João Silva",
      "email": "joao@email.com",
      "phone": "11999999999"
    },
    "products": [
      {
        "name": "E-book Java",
        "price": 49.90,
        "quantity": 2
      }
    ],
    "shippingAddress": "Rua Exemplo, 123"
  }'
Listar todos os pedidos
bash
curl http://localhost:8080/api/orders
Atualizar status
bash
curl -X PUT "http://localhost:8080/api/orders/ORD-123/status?status=PROCESSING"
📂 Estrutura do Projeto
text
orderflow/
├── src/main/java/com/orderflow/
│   ├── OrderFlowComplete.java     # Arquivo principal com todo o código
│   ├── config/                    # Configurações
│   ├── controller/                # Endpoints da API
│   ├── domain/                    # Modelos e enums
│   ├── application/               # Padrões de projeto
│   │   ├── builder/              # Builder Pattern
│   │   ├── chain/                # Chain of Responsibility
│   │   ├── factory/              # Factory Pattern
│   │   ├── observer/             # Observer Pattern
│   │   └── strategy/             # Strategy Pattern
│   └── infrastructure/            # Repositório e serviço
├── src/main/resources/
│   └── application.properties     # Configurações
└── pom.xml                        # Dependências
🧪 Testar
bash
mvn test
🤝 Como contribuir
Faça um fork do projeto

Crie uma branch: git checkout -b minha-feature

Commit: git commit -m 'Minha feature'

Push: git push origin minha-feature

Abra um Pull Request

📄 Licença
MIT - veja o arquivo LICENSE

👤 Autor
Erivelton Rocha

GitHub: @EriveltonRocha77


⭐ Se gostou, deixe uma estrela!
Feito com ❤️






