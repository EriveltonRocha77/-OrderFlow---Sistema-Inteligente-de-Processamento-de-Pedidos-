package com.orderflow;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EntityScan(basePackages = "com.orderflow")
@EnableJpaRepositories(basePackages = "com.orderflow")
@Slf4j
public class OrderFlowComplete {
    public static void main(String[] args) {
        SpringApplication.run(OrderFlowComplete.class, args);

        System.out.println("""

                ╔══════════════════════════════════════════════════════════════════╗
                ║                                                                  ║
                ║   🚀 ORDERFLOW - Sistema de Processamento de Pedidos           ║
                ║                                                                  ║
                ║   📌 API disponível em: http://localhost:8080                   ║
                ║   📊 H2 Console: http://localhost:8080/h2-console              ║
                ║                                                                  ║
                ║   🎯 Design Patterns Implementados:                            ║
                ║   • Builder Pattern - Construção de objetos complexos          ║
                ║   • Factory Pattern - Criação de diferentes tipos de pedido    ║
                ║   • Strategy Pattern - Algoritmos intercambiáveis (frete)      ║
                ║   • Chain of Responsibility - Pipeline de validações           ║
                ║   • Observer Pattern - Notificações de status                  ║
                ║   • Singleton Pattern - Gerenciador de configurações           ║
                ║                                                                  ║
                ║   📝 TESTE RÁPIDO:                                              ║
                ║   POST http://localhost:8080/api/orders/digital                ║
                ║   POST http://localhost:8080/api/orders/physical               ║
                ║   GET  http://localhost:8080/api/orders                        ║
                ║                                                                  ║
                ╚══════════════════════════════════════════════════════════════════╝
                """);

        ConfigurationManager config = ConfigurationManager.getInstance();
        log.info("📋 Configurações carregadas: {}", config.getAllConfigs());
    }
}

@Configuration
class AppConfig {
    @Bean
    public OrderValidationHandler validationChain() {
        OrderValidationHandler stockHandler = new StockValidationHandler();
        OrderValidationHandler paymentHandler = new PaymentValidationHandler();
        OrderValidationHandler fraudHandler = new FraudValidationHandler();
        OrderValidationHandler shippingHandler = new ShippingValidationHandler();

        stockHandler.setNext(paymentHandler);
        paymentHandler.setNext(fraudHandler);
        fraudHandler.setNext(shippingHandler);

        return stockHandler;
    }

    @Bean
    public ShippingStrategy shippingStrategy() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        String shippingType = config.getConfig("shipping.default", "STANDARD");
        return "EXPRESS".equalsIgnoreCase(shippingType) ? new ExpressShipping() : new StandardShipping();
    }

    @Bean
    public OrderSubject orderSubject() {
        OrderSubject subject = new OrderSubject();
        ConfigurationManager config = ConfigurationManager.getInstance();

        if ("true".equalsIgnoreCase(config.getConfig("notification.email.enabled", "true"))) {
            subject.attach(new EmailNotifier());
        }
        if ("true".equalsIgnoreCase(config.getConfig("notification.sms.enabled", "true"))) {
            subject.attach(new SmsNotifier());
        }
        return subject;
    }

    @Bean
    public OrderProcessingService orderProcessingService(
            OrderRepository orderRepository,
            OrderValidationHandler validationChain,
            ShippingStrategy shippingStrategy,
            OrderSubject orderSubject) {
        return new OrderProcessingService(orderRepository, validationChain, shippingStrategy, orderSubject);
    }

}

enum OrderStatus {
    PENDING("Pendente", "🟡"),
    PROCESSING("Processando", "🔄"),
    VALIDATED("Validado", "✅"),
    SHIPPED("Enviado", "🚚"),
    DELIVERED("Entregue", "📦"),
    CANCELLED("Cancelado", "❌"),
    REFUNDED("Reembolsado", "💰");

    private final String description;
    private final String icon;

    OrderStatus(String description, String icon) {
        this.description = description;
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }

    public boolean isProcessing() {
        return this == PROCESSING || this == VALIDATED || this == SHIPPED;
    }
}

enum PaymentMethod {
    CREDIT_CARD("Cartão de Crédito", "💳"),
    DEBIT_CARD("Cartão de Débito", "💳"),
    PIX("PIX", "📱"),
    BOLETO("Boleto Bancário", "📄"),
    DIGITAL_WALLET("Carteira Digital", "📱"),
    CRYPTO("Criptomoeda", "₿");

    private final String description;
    private final String icon;

    PaymentMethod(String description, String icon) {
        this.description = description;
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String document;
    private String address;
    private LocalDateTime registrationDate;

    public Customer(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.registrationDate = LocalDateTime.now();
    }

    public String getContactInfo() {
        return String.format("%s (%s)", name, email);
    }
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class Product {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private Integer stockQuantity;
    private Double weight;
    private String category;
    private Boolean isDigital;
    private String sku;

    public Product(String name, Double price, Integer quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.stockQuantity = 100;
        this.weight = 1.0;
        this.isDigital = false;
        this.sku = "SKU-" + System.currentTimeMillis();
    }

    public Double getTotalPrice() {
        return price * quantity;
    }
}

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Order {
    @Id
    private String id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "customer_id")),
            @AttributeOverride(name = "name", column = @Column(name = "customer_name")),
            @AttributeOverride(name = "email", column = @Column(name = "customer_email")),
            @AttributeOverride(name = "phone", column = @Column(name = "customer_phone")),
            @AttributeOverride(name = "document", column = @Column(name = "customer_document")),
            @AttributeOverride(name = "address", column = @Column(name = "customer_address")),
            @AttributeOverride(name = "registrationDate", column = @Column(name = "customer_registration_date"))
    })
    private Customer customer;

    @ElementCollection
    @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "product_id")),
            @AttributeOverride(name = "name", column = @Column(name = "product_name")),
            @AttributeOverride(name = "description", column = @Column(name = "product_description")),
            @AttributeOverride(name = "price", column = @Column(name = "product_price")),
            @AttributeOverride(name = "quantity", column = @Column(name = "product_quantity")),
            @AttributeOverride(name = "stockQuantity", column = @Column(name = "product_stock_quantity")),
            @AttributeOverride(name = "weight", column = @Column(name = "product_weight")),
            @AttributeOverride(name = "category", column = @Column(name = "product_category")),
            @AttributeOverride(name = "isDigital", column = @Column(name = "product_is_digital")),
            @AttributeOverride(name = "sku", column = @Column(name = "product_sku"))
    })
    private List<Product> products = new ArrayList<>();

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Double subtotal;
    private Double discount;
    private Double shippingCost;
    private Double total;
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String paymentId;
    private String notes;

    @ElementCollection
    private List<String> trackingHistory = new ArrayList<>();

    @Transient
    private Map<String, Object> metadata = new HashMap<>();

    public void calculateTotal() {
        this.subtotal = products.stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum();
        this.discount = this.discount != null ? this.discount : 0.0;
        this.shippingCost = this.shippingCost != null ? this.shippingCost : 0.0;
        this.total = this.subtotal - this.discount + this.shippingCost;
    }

    public void addTrackingEvent(String event) {
        if (trackingHistory == null) {
            trackingHistory = new ArrayList<>();
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        trackingHistory.add(String.format("[%s] %s", timestamp, event));
    }

    public String getSummary() {
        return String.format("Pedido #%s - %s - Total: R$ %.2f - Status: %s",
                id, customer.getName(), total, status.getDescription());
    }

    public boolean isDigital() {
        return products.stream().allMatch(Product::getIsDigital);
    }
}

class OrderBuilder {
    private String id;
    private Customer customer;
    private List<Product> products = new ArrayList<>();
    private LocalDateTime orderDate;
    private OrderStatus status;
    private Double discount = 0.0;
    private Double shippingCost = 0.0;
    private String shippingAddress;
    private PaymentMethod paymentMethod;
    private String notes;
    private Map<String, Object> metadata = new HashMap<>();

    public OrderBuilder id(String id) {
        this.id = id;
        return this;
    }

    public OrderBuilder customer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public OrderBuilder addProduct(Product product) {
        this.products.add(product);
        return this;
    }

    public OrderBuilder addProducts(List<Product> products) {
        this.products.addAll(products);
        return this;
    }

    public OrderBuilder orderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
        return this;
    }

    public OrderBuilder status(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderBuilder discount(Double discount) {
        this.discount = discount != null ? discount : 0.0;
        return this;
    }

    public OrderBuilder shippingCost(Double shippingCost) {
        this.shippingCost = shippingCost != null ? shippingCost : 0.0;
        return this;
    }

    public OrderBuilder shippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public OrderBuilder paymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public OrderBuilder notes(String notes) {
        this.notes = notes;
        return this;
    }

    public OrderBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public OrderBuilder metadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public Order build() {
        Order order = new Order();
        order.setId(this.id != null ? this.id : generateOrderId());
        order.setCustomer(this.customer);
        order.setProducts(this.products);
        order.setOrderDate(this.orderDate != null ? this.orderDate : LocalDateTime.now());
        order.setStatus(this.status != null ? this.status : OrderStatus.PENDING);
        order.setDiscount(this.discount);
        order.setShippingCost(this.shippingCost);
        order.setShippingAddress(this.shippingAddress);
        order.setPaymentMethod(this.paymentMethod);
        order.setNotes(this.notes);
        order.setMetadata(this.metadata);
        order.calculateTotal();
        order.addTrackingEvent("Pedido criado");
        return order;
    }

    private String generateOrderId() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", new Random().nextInt(10000));
        return prefix + "-" + timestamp.substring(timestamp.length() - 6) + "-" + random;
    }

    public static Order createSimpleOrder(Customer customer, List<Product> products, String address) {
        return new OrderBuilder()
                .customer(customer)
                .addProducts(products)
                .shippingAddress(address)
                .paymentMethod(PaymentMethod.PIX)
                .build();
    }

    public static Order createExpressOrder(Customer customer, List<Product> products, String address) {
        return new OrderBuilder()
                .customer(customer)
                .addProducts(products)
                .shippingAddress(address)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .shippingCost(25.0)
                .notes("URGENTE - Entrega Expressa")
                .metadata("deliveryType", "EXPRESS")
                .build();
    }
}

class ValidationException extends Exception {
    private String errorCode;
    private Map<String, Object> details;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public ValidationException withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }
}

abstract class OrderValidationHandler {
    protected OrderValidationHandler next;
    protected String handlerName;

    public OrderValidationHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    public void setNext(OrderValidationHandler next) {
        this.next = next;
    }

    public abstract void validate(Order order) throws ValidationException;

    protected void validateNext(Order order) throws ValidationException {
        if (next != null) {
            next.validate(order);
        }
    }

    protected void logValidation(String message, Object... args) {
        String formatted = String.format(message, args);
        System.out.printf("[%s] %s%n", handlerName, formatted);
    }
}

@Slf4j
class StockValidationHandler extends OrderValidationHandler {
    public StockValidationHandler() {
        super("VALIDACAO_ESTOQUE");
    }

    @Override
    public void validate(Order order) throws ValidationException {
        logValidation("Validando estoque...");

        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new ValidationException("Pedido não possui produtos", "EMPTY_ORDER");
        }

        Map<String, Object> details = new HashMap<>();

        for (Product product : order.getProducts()) {
            if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
                details.put(product.getName(), "Sem estoque");
                throw new ValidationException("Produto sem estoque: " + product.getName(), "OUT_OF_STOCK")
                        .withDetails(details);
            }
            if (product.getQuantity() == null || product.getQuantity() <= 0) {
                details.put(product.getName(), "Quantidade inválida");
                throw new ValidationException("Quantidade inválida para: " + product.getName(), "INVALID_QUANTITY")
                        .withDetails(details);
            }
            if (product.getStockQuantity() < product.getQuantity()) {
                details.put(product.getName(), String.format("Disponível: %d, Solicitado: %d",
                        product.getStockQuantity(), product.getQuantity()));
                throw new ValidationException(
                        String.format("Estoque insuficiente para %s", product.getName()),
                        "INSUFFICIENT_STOCK"
                ).withDetails(details);
            }
        }

        log.info("✅ Validação de estoque concluída com sucesso");
        validateNext(order);
    }
}

@Slf4j
class PaymentValidationHandler extends OrderValidationHandler {
    public PaymentValidationHandler() {
        super("VALIDACAO_PAGAMENTO");
    }

    @Override
    public void validate(Order order) throws ValidationException {
        logValidation("Validando pagamento...");

        if (order.getPaymentMethod() == null) {
            throw new ValidationException("Método de pagamento não informado", "NO_PAYMENT_METHOD");
        }

        if (order.getTotal() == null || order.getTotal() <= 0) {
            throw new ValidationException("Valor do pedido inválido", "INVALID_TOTAL");
        }

        ConfigurationManager config = ConfigurationManager.getInstance();
        double maxOrderValue = Double.parseDouble(config.getConfig("max.order.value", "10000"));

        if (order.getTotal() > maxOrderValue) {
            throw new ValidationException(
                    String.format("Valor do pedido excede o limite permitido (R$ %.2f)", maxOrderValue),
                    "ORDER_LIMIT_EXCEEDED"
            );
        }

        log.info("✅ Validação de pagamento concluída com sucesso");
        validateNext(order);
    }
}

@Slf4j
class FraudValidationHandler extends OrderValidationHandler {
    private final Set<String> blockedDocuments = new HashSet<>(Arrays.asList(
            "000.000.000-00", "111.111.111-11", "123.456.789-00"
    ));

    private final Set<String> suspiciousAddresses = new HashSet<>(Arrays.asList(
            "Rua Teste", "Rua Suspeita", "Endereço Falso"
    ));

    public FraudValidationHandler() {
        super("VALIDACAO_ANTIFRAUDE");
    }

    @Override
    public void validate(Order order) throws ValidationException {
        logValidation("Validando fraudes...");

        if (order.getCustomer() == null) {
            throw new ValidationException("Cliente não informado", "NO_CUSTOMER");
        }

        if (order.getCustomer().getDocument() != null) {
            String doc = order.getCustomer().getDocument().replaceAll("[^0-9]", "");
            if (blockedDocuments.contains(doc)) {
                throw new ValidationException("Cliente bloqueado por suspeita de fraude", "BLOCKED_CUSTOMER");
            }
        }

        if (order.getShippingAddress() != null) {
            for (String suspicious : suspiciousAddresses) {
                if (order.getShippingAddress().toLowerCase().contains(suspicious.toLowerCase())) {
                    log.warn("⚠️ Endereço suspeito identificado: {}", order.getShippingAddress());
                    throw new ValidationException("Endereço suspeito detectado", "SUSPICIOUS_ADDRESS");
                }
            }
        }

        Map<String, Long> productCount = order.getProducts().stream()
                .collect(Collectors.groupingBy(Product::getName, Collectors.counting()));

        for (Map.Entry<String, Long> entry : productCount.entrySet()) {
            if (entry.getValue() > 10) {
                log.warn("⚠️ Quantidade excessiva do produto {}: {}", entry.getKey(), entry.getValue());
            }
        }

        log.info("✅ Validação antifraude concluída com sucesso");
        validateNext(order);
    }
}

@Slf4j
class ShippingValidationHandler extends OrderValidationHandler {
    public ShippingValidationHandler() {
        super("VALIDACAO_FRETE");
    }

    @Override
    public void validate(Order order) throws ValidationException {
        logValidation("Validando informações de frete...");

        if (!order.isDigital()) {
            if (order.getShippingAddress() == null || order.getShippingAddress().isEmpty()) {
                throw new ValidationException("Endereço de entrega não informado para produtos físicos", "NO_SHIPPING_ADDRESS");
            }
            if (order.getShippingAddress().length() < 10) {
                throw new ValidationException("Endereço de entrega inválido (muito curto)", "INVALID_SHIPPING_ADDRESS");
            }
        } else {
            log.info("✅ Produto digital - Sem necessidade de endereço físico");
        }

        log.info("✅ Validação de frete concluída com sucesso");
        validateNext(order);
    }
}

interface OrderFactory {
    Order createOrder(Customer customer, List<Product> products, String address);
    default String getOrderType() {
        return "GENERIC";
    }
    default String getDescription() {
        return "Fábrica genérica de pedidos";
    }
}

@Slf4j
class DigitalOrderFactory implements OrderFactory {
    @Override
    public Order createOrder(Customer customer, List<Product> products, String address) {
        log.info("📱 Criando pedido digital para: {}", customer.getName());

        products.forEach(p -> {
            p.setIsDigital(true);
            p.setWeight(0.0);
        });

        double subtotal = products.stream().mapToDouble(Product::getTotalPrice).sum();
        ConfigurationManager config = ConfigurationManager.getInstance();
        double discountPercent = Double.parseDouble(config.getConfig("discount.digital.percent", "10"));
        double discount = subtotal * (discountPercent / 100);

        return new OrderBuilder()
                .customer(customer)
                .addProducts(products)
                .shippingAddress("📧 Entrega Digital - Produto enviado por email")
                .paymentMethod(PaymentMethod.DIGITAL_WALLET)
                .discount(discount)
                .shippingCost(0.0)
                .notes("Produto digital - Entrega imediata por email")
                .metadata("deliveryType", "DIGITAL")
                .metadata("discountApplied", discountPercent + "%")
                .build();
    }

    @Override
    public String getOrderType() {
        return "DIGITAL";
    }

    @Override
    public String getDescription() {
        return "Fábrica de produtos digitais com desconto de 10%";
    }
}

@Slf4j
class PhysicalOrderFactory implements OrderFactory {
    @Override
    public Order createOrder(Customer customer, List<Product> products, String address) {
        log.info("📦 Criando pedido físico para: {}", customer.getName());
        products.forEach(p -> p.setIsDigital(false));

        return new OrderBuilder()
                .customer(customer)
                .addProducts(products)
                .shippingAddress(address)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .discount(0.0)
                .notes("Produto físico - Entrega via transportadora")
                .metadata("deliveryType", "PHYSICAL")
                .metadata("estimatedDelivery", "5-7 dias úteis")
                .build();
    }

    @Override
    public String getOrderType() {
        return "PHYSICAL";
    }

    @Override
    public String getDescription() {
        return "Fábrica de produtos físicos com entrega por transportadora";
    }
}

@Slf4j
class PremiumOrderFactory implements OrderFactory {
    @Override
    public Order createOrder(Customer customer, List<Product> products, String address) {
        log.info("⭐ Criando pedido premium para: {}", customer.getName());

        double subtotal = products.stream().mapToDouble(Product::getTotalPrice).sum();
        double discount = subtotal * 0.15;

        return new OrderBuilder()
                .customer(customer)
                .addProducts(products)
                .shippingAddress(address)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .discount(discount)
                .shippingCost(0.0)
                .notes("✨ Pedido Premium - Benefícios exclusivos")
                .metadata("deliveryType", "PREMIUM")
                .metadata("priority", "HIGH")
                .metadata("benefits", "Frete grátis, 15% desconto, Embalagem especial")
                .build();
    }

    @Override
    public String getOrderType() {
        return "PREMIUM";
    }

    @Override
    public String getDescription() {
        return "Fábrica de pedidos premium com benefícios exclusivos";
    }
}

interface OrderObserver {
    void update(Order order, OrderStatus oldStatus, OrderStatus newStatus);
    String getObserverType();
    default boolean isEnabled() {
        return true;
    }
}

@Slf4j
class EmailNotifier implements OrderObserver {
    @Override
    public void update(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        String subject = getEmailSubject(newStatus);
        String body = getEmailBody(order, oldStatus, newStatus);
        String email = order.getCustomer().getEmail();

        log.info("""

                ╔══════════════════════════════════════════════════════════╗
                ║                    📧 EMAIL ENVIADO                     ║
                ╠══════════════════════════════════════════════════════════╣
                ║ Para: %-40s ║
                ║ Assunto: %-40s ║
                ╠══════════════════════════════════════════════════════════╣
                ║ %-58s ║
                ╚══════════════════════════════════════════════════════════╝
                """,
                email.length() > 40 ? email.substring(0, 37) + "..." : email,
                subject.length() > 40 ? subject.substring(0, 37) + "..." : subject,
                body.lines().findFirst().orElse("")
        );

        log.debug("Corpo do email:\n{}", body);
    }

    private String getEmailSubject(OrderStatus status) {
        return switch (status) {
            case PENDING -> "🛒 Pedido Recebido com Sucesso!";
            case PROCESSING -> "⚙️ Seu pedido está sendo processado";
            case VALIDATED -> "✅ Pedido Validado";
            case SHIPPED -> "🚚 Seu pedido foi enviado!";
            case DELIVERED -> "📦 Pedido Entregue!";
            case CANCELLED -> "❌ Pedido Cancelado";
            case REFUNDED -> "💰 Reembolso Processado";
            default -> "📢 Atualização do Pedido";
        };
    }

    private String getEmailBody(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        String statusIcon = newStatus.getIcon();
        String oldStatusText = oldStatus != null ? oldStatus.getDescription() : "NOVO PEDIDO";

        return String.format("""
                Olá %s! %s

                Seu pedido #%s foi atualizado!

                Status anterior: %s
                Novo status: %s %s

                ──── Detalhes do Pedido ────
                Total: R$ %.2f
                Produtos: %d itens
                Subtotal: R$ %.2f
                Desconto: R$ %.2f
                Frete: R$ %.2f
                Endereço: %s

                %s

                Atenciosamente,
                Equipe OrderFlow
                """,
                order.getCustomer().getName(),
                statusIcon,
                order.getId(),
                oldStatusText,
                newStatus.getDescription(),
                statusIcon,
                order.getTotal(),
                order.getProducts().size(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingCost(),
                order.getShippingAddress(),
                order.getNotes() != null ? "📝 " + order.getNotes() : ""
        );
    }

    @Override
    public String getObserverType() {
        return "EMAIL";
    }
}

@Slf4j
class SmsNotifier implements OrderObserver {
    @Override
    public void update(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        String message = String.format("""
                %s OrderFlow: Pedido #%s %s -> %s. Total: R$ %.2f
                """,
                newStatus.getIcon(),
                order.getId().substring(0, Math.min(8, order.getId().length())),
                oldStatus != null ? oldStatus.name() : "NOVO",
                newStatus.name(),
                order.getTotal()
        );

        log.info("""

                ╔══════════════════════════════════════════════════════════╗
                ║                    📱 SMS ENVIADO                      ║
                ╠══════════════════════════════════════════════════════════╣
                ║ Para: %-40s ║
                ║ Mensagem: %-40s ║
                ╚══════════════════════════════════════════════════════════╝
                """,
                order.getCustomer().getPhone(),
                message.substring(0, Math.min(40, message.length()))
        );
    }

    @Override
    public String getObserverType() {
        return "SMS";
    }
}

@Slf4j
class PushNotifier implements OrderObserver {
    @Override
    public void update(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        String message = String.format("%s Pedido #%s: %s",
                newStatus.getIcon(),
                order.getId().substring(0, Math.min(8, order.getId().length())),
                newStatus.getDescription()
        );

        log.info("""

                📲 PUSH NOTIFICATION
                ────────────────────────────────────────────────────────
                Usuário: {}
                Mensagem: {}
                ────────────────────────────────────────────────────────
                """,
                order.getCustomer().getName(),
                message
        );
    }

    @Override
    public String getObserverType() {
        return "PUSH";
    }
}

@Slf4j
class OrderSubject {
    private final List<OrderObserver> observers = new ArrayList<>();
    private final Map<String, List<OrderObserver>> observersByType = new HashMap<>();

    public void attach(OrderObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            observersByType.computeIfAbsent(observer.getObserverType(), k -> new ArrayList<>()).add(observer);
            log.info("📌 Observador {} registrado", observer.getObserverType());
        }
    }

    public void detach(OrderObserver observer) {
        observers.remove(observer);
        observersByType.values().forEach(list -> list.remove(observer));
        log.info("📌 Observador {} removido", observer.getObserverType());
    }

    public void detachByType(String type) {
        List<OrderObserver> toRemove = observersByType.getOrDefault(type, new ArrayList<>());
        observers.removeAll(toRemove);
        observersByType.remove(type);
        log.info("📌 Removidos {} observadores do tipo {}", toRemove.size(), type);
    }

    public void notifyObservers(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        log.info("🔔 Notificando {} observadores sobre mudança de status...", observers.size());
        int notified = 0;

        for (OrderObserver observer : observers) {
            try {
                if (observer.isEnabled()) {
                    observer.update(order, oldStatus, newStatus);
                    notified++;
                }
            } catch (Exception e) {
                log.error("Erro ao notificar observador {}: {}", observer.getObserverType(), e.getMessage());
            }
        }

        log.info("✅ {} observadores notificados com sucesso", notified);
    }

    public int getObserverCount() {
        return observers.size();
    }
}

interface ShippingStrategy {
    double calculate(Order order);
    String getName();
}

class StandardShipping implements ShippingStrategy {
    @Override
    public double calculate(Order order) {
        if (order.isDigital()) {
            return 0.0;
        }
        double base = 15.0;
        double weightCost = order.getProducts().stream()
                .mapToDouble(p -> p.getWeight() != null ? p.getWeight() * 2.5 : 0.0)
                .sum();
        return base + weightCost;
    }

    @Override
    public String getName() {
        return "STANDARD";
    }
}

class ExpressShipping implements ShippingStrategy {
    @Override
    public double calculate(Order order) {
        if (order.isDigital()) {
            return 0.0;
        }
        double base = 25.0;
        double urgency = order.getProducts().stream()
                .mapToDouble(p -> p.getWeight() != null ? p.getWeight() * 3.0 : 0.0)
                .sum();
        return base + urgency;
    }

    @Override
    public String getName() {
        return "EXPRESS";
    }
}

class ConfigurationManager {
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    private final Map<String, String> configs = new HashMap<>();

    private ConfigurationManager() {
        configs.put("shipping.default", "STANDARD");
        configs.put("notification.email.enabled", "true");
        configs.put("notification.sms.enabled", "true");
        configs.put("discount.digital.percent", "10");
        configs.put("max.order.value", "10000");
    }

    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    public String getConfig(String key, String defaultValue) {
        return configs.getOrDefault(key, defaultValue);
    }

    public Map<String, String> getAllConfigs() {
        return new HashMap<>(configs);
    }
}

@RestController
@RequestMapping("/api")
class OrderController {
    private final OrderProcessingService orderProcessingService;

    public OrderController(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @PostMapping("/orders/{type}")
    public ResponseEntity<Map<String, Object>> createOrder(@PathVariable String type,
                                                           @RequestBody OrderRequest request) {
        try {
            Order order = orderProcessingService.createOrder(type, request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pedido criado com sucesso");
            response.put("order", order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/orders")
    public List<Order> listOrders() {
        return orderProcessingService.listOrders();
    }
}

class OrderProcessingService {
    private final OrderRepository orderRepository;
    private final OrderValidationHandler validationChain;
    private final ShippingStrategy shippingStrategy;
    private final OrderSubject orderSubject;

    public OrderProcessingService(OrderRepository orderRepository,
                                 OrderValidationHandler validationChain,
                                 ShippingStrategy shippingStrategy,
                                 OrderSubject orderSubject) {
        this.orderRepository = orderRepository;
        this.validationChain = validationChain;
        this.shippingStrategy = shippingStrategy;
        this.orderSubject = orderSubject;
    }

    public Order createOrder(String type, OrderRequest request) throws ValidationException {
        String normalizedType = type == null ? "physical" : type.toLowerCase(Locale.ROOT);
        Customer customer = new Customer(
                request.customerName,
                request.customerEmail,
                request.customerPhone
        );
        customer.setId(UUID.randomUUID().toString());
        customer.setDocument(request.customerDocument);
        customer.setAddress(request.address);

        List<Product> products = request.products == null ? new ArrayList<>() : request.products.stream()
                .map(p -> new Product(p.name, p.price, p.quantity))
                .collect(Collectors.toList());

        OrderFactory factory = resolveFactory(normalizedType);
        Order order = factory.createOrder(customer, products, request.address);

        if (request.paymentMethod != null) {
            order.setPaymentMethod(PaymentMethod.valueOf(request.paymentMethod.trim().toUpperCase(Locale.ROOT)));
        }

        order.setShippingCost(shippingStrategy.calculate(order));
        order.calculateTotal();
        validationChain.validate(order);

        order.setStatus(OrderStatus.PROCESSING);
        order.addTrackingEvent("Pedido validado e em processamento");
        orderRepository.save(order);
        orderSubject.notifyObservers(order, null, OrderStatus.PROCESSING);

        order.setStatus(OrderStatus.VALIDATED);
        order.addTrackingEvent("Pedido aprovado pela validação final");
        orderRepository.save(order);
        orderSubject.notifyObservers(order, OrderStatus.PROCESSING, OrderStatus.VALIDATED);

        return order;
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    private OrderFactory resolveFactory(String type) {
        return switch (type) {
            case "digital" -> new DigitalOrderFactory();
            case "premium" -> new PremiumOrderFactory();
            default -> new PhysicalOrderFactory();
        };
    }
}

interface OrderRepository extends JpaRepository<Order, String> {
}

class OrderRequest {
    public String customerName;
    public String customerEmail;
    public String customerPhone;
    public String customerDocument;
    public String address;
    public String paymentMethod;
    public List<OrderProductRequest> products;
}

class OrderProductRequest {
    public String name;
    public Double price;
    public Integer quantity;
    public Integer stockQuantity;
    public Double weight;
    public String category;
}
