const API = {
    products: "/api/products",
    customers: "/api/customers",
    sales: "/api/sales",
    createSale: "/api/sales/create",
    createMultipleSale: "/api/sales/create-multiple",
    saleItems: "/api/sale-items"
};

let products = [];
let customers = [];
let sales = [];
let cart = new Map();
let selectedCategory = "all";
let toastTimer;
let currentInvoice = null;

document.addEventListener("DOMContentLoaded", async () => {
    const currentDate = document.getElementById("currentDate");
    const currentYear = document.getElementById("currentYear");
    const productSearch = document.getElementById("productSearch");

    if (currentDate) {
        currentDate.textContent = new Date().toLocaleDateString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        });
    }

    if (currentYear) {
        currentYear.textContent = new Date().getFullYear();
    }

    if (productSearch) {
        productSearch.addEventListener("input", renderProducts);
    }

    await loadDashboard();

    document.querySelectorAll(".nav-item").forEach(link => {
        link.addEventListener("click", () => {
            document.querySelectorAll(".nav-item")
                .forEach(item => item.classList.remove("active"));

            link.classList.add("active");
        });
    });
});


/* API REQUEST */

async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    const text = await response.text();
    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (!response.ok) {
        const message = typeof data === "string"
            ? data
            : data?.error || data?.message ||
              `Request failed (${response.status})`;

        throw new Error(message);
    }

    return data;
}


/* DASHBOARD */

async function loadDashboard() {
    await Promise.allSettled([
        loadProducts(),
        loadCustomers(),
        loadSales()
    ]);
}


/* LOAD PRODUCTS */

async function loadProducts() {
    try {
        products = await apiRequest(API.products);

        document.getElementById("totalProducts").textContent =
            products.length;

        document.getElementById("lowStockProducts").textContent =
            products.filter(product =>
                Number(product.quantity) < 5
            ).length;

        buildCategoryFilters();
        renderProducts();
        renderCart();

    } catch (error) {
        document.getElementById("productGrid").innerHTML = `
            <div class="empty-products">
                Unable to load products.<br>
                ${escapeHtml(error.message)}
            </div>
        `;

        showToast("Products could not be loaded.", true);
    }
}


/* LOAD CUSTOMERS */

async function loadCustomers() {
    const select = document.getElementById("customerSelect");

    try {
        customers = await apiRequest(API.customers);

        document.getElementById("totalCustomers").textContent =
            customers.length;

        const previousValue = select.value;

        select.innerHTML = `
            <option value="">Select a customer</option>

            ${customers.map(customer => `
                <option value="${Number(customer.customerId)}">
                    ${escapeHtml(customer.customerName)}
                    ${
                        customer.phone
                            ? " · " + escapeHtml(customer.phone)
                            : ""
                    }
                </option>
            `).join("")}
        `;

        if (previousValue) {
            select.value = previousValue;
        }

    } catch (error) {
        select.innerHTML = `
            <option value="">Unable to load customers</option>
        `;

        showToast("Customers could not be loaded.", true);
    }
}


/* LOAD SALES */

async function loadSales() {
    const tbody = document.getElementById("salesTableBody");

    try {
        sales = await apiRequest(API.sales);

        document.getElementById("totalSales").textContent =
            sales.length;

        const latestSales = [...sales]
            .sort((a, b) => Number(b.saleId) - Number(a.saleId))
            .slice(0, 7);

        if (latestSales.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="table-loading">
                        No sales recorded yet.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = latestSales.map(sale => `
            <tr>
                <td class="sale-id">#${Number(sale.saleId)}</td>

                <td>
                    ${escapeHtml(
                        sale.customer?.customerName || "Customer"
                    )}
                </td>

                <td>${formatDate(sale.saleDate)}</td>

                <td>
                    <span class="sale-status">Recorded</span>
                </td>
            </tr>
        `).join("");

    } catch (error) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="table-loading">
                    Unable to load sales.
                </td>
            </tr>
        `;
    }
}


/* PRODUCT CATEGORIES */

function buildCategoryFilters() {
    const container = document.getElementById("categoryFilters");

    const categories = [...new Set(
        products
            .map(product => product.category)
            .filter(category => category && category.trim())
    )].sort();

    const allCategories = ["all", ...categories];

    if (!allCategories.includes(selectedCategory)) {
        selectedCategory = "all";
    }

    container.innerHTML = allCategories.map(category => `
        <button
            class="category-chip ${
                selectedCategory === category ? "active" : ""
            }"
            data-category="${escapeHtml(category)}"
            onclick="selectCategory('${escapeJs(category)}')"
        >
            ${
                category === "all"
                    ? "All products"
                    : escapeHtml(category)
            }
        </button>
    `).join("");
}


function selectCategory(category) {
    selectedCategory = category;
    buildCategoryFilters();
    renderProducts();
}


/* PRODUCT EMOJIS */

function productEmoji(product) {
    const text = (
        (product.productName || "") + " " +
        (product.category || "")
    ).toLowerCase();

    const rules = [
        [/rice|grain/, "🍚"],
        [/oil/, "🫗"],
        [/milk|dairy|curd|yogurt/, "🥛"],
        [/sugar/, "🧂"],
        [/biscuit|cookie|snack|chips/, "🍪"],
        [/soap|shampoo|detergent|clean/, "🧼"],
        [/coffee/, "☕"],
        [/tea/, "🍵"],
        [/fruit|apple|banana|orange|mango/, "🍎"],
        [/vegetable|tomato|potato|onion/, "🥦"],
        [/bread|bakery/, "🍞"],
        [/chocolate/, "🍫"],
        [/egg/, "🥚"],
        [/water|drink|juice/, "🥤"],
        [/meat|chicken/, "🍗"],
        [/frozen|ice cream/, "🍦"]
    ];

    for (const [pattern, emoji] of rules) {
        if (pattern.test(text)) {
            return emoji;
        }
    }

    return "🛍️";
}


function productBackground(product) {
    const colors = [
        "#eef8e9",
        "#fff3df",
        "#eaf3ff",
        "#f9eafa",
        "#e9f8f5",
        "#fff0eb"
    ];

    return colors[
        Math.abs(Number(product.productId) || 0) % colors.length
    ];
}


/* DISPLAY PRODUCTS */

function renderProducts() {
    const container = document.getElementById("productGrid");

    const search = document.getElementById("productSearch")
        .value.trim().toLowerCase();

    const filteredProducts = products.filter(product => {
        const matchesSearch =
            (product.productName || "").toLowerCase().includes(search) ||
            (product.category || "").toLowerCase().includes(search);

        const matchesCategory =
            selectedCategory === "all" ||
            product.category === selectedCategory;

        return matchesSearch && matchesCategory;
    });

    document.getElementById("productCount").textContent =
        `${filteredProducts.length} items`;

    if (filteredProducts.length === 0) {
        container.innerHTML = `
            <div class="empty-products">
                No matching products found.
            </div>
        `;
        return;
    }

    container.innerHTML = filteredProducts.map(product => {
        const stock = Number(product.quantity) || 0;

        const inCart =
            cart.get(Number(product.productId))?.quantity || 0;

        let stockClass = "";
        let stockLabel = `${stock} in stock`;

        if (stock <= 0) {
            stockClass = "out";
            stockLabel = "Out of stock";
        } else if (stock < 5) {
            stockClass = "low";
            stockLabel = `Only ${stock} left`;
        }

        return `
            <article class="product-card">

                <div
                    class="product-visual"
                    style="background:${productBackground(product)}"
                >
                    <span class="product-category-label">
                        ${escapeHtml(product.category || "General")}
                    </span>

                    <span class="product-emoji">
                        ${productEmoji(product)}
                    </span>
                </div>

                <div class="product-info">

                    <h3 class="product-name">
                        ${escapeHtml(product.productName)}
                    </h3>

                    <div class="product-stock ${stockClass}">
                        ${stockLabel}
                    </div>

                    <div class="product-bottom">

                        <span class="product-price">
                            ${money(product.price)}
                        </span>

                        <button
                            class="add-button"
                            onclick="addToCart(${Number(product.productId)})"
                            ${stock <= inCart ? "disabled" : ""}
                        >
                            ${inCart > 0 ? `Added ${inCart}` : "＋ Add"}
                        </button>

                    </div>
                </div>
            </article>
        `;
    }).join("");
}


/* ADD PRODUCT TO CART */

function addToCart(productId) {
    const product = products.find(
        item => Number(item.productId) === Number(productId)
    );

    if (!product) {
        return showToast("Product not found.", true);
    }

    const stock = Number(product.quantity) || 0;

    if (stock <= 0) {
        return showToast("This product is out of stock.", true);
    }

    const current = cart.get(Number(productId));

    const currentQuantity = current
        ? current.quantity
        : 0;

    if (currentQuantity >= stock) {
        return showToast("Available stock limit reached.", true);
    }

    cart.set(Number(productId), {
        product,
        quantity: currentQuantity + 1
    });

    renderProducts();
    renderCart();
}


/* CHANGE CART QUANTITY */

function changeQuantity(productId, difference) {
    const item = cart.get(Number(productId));

    if (!item) {
        return;
    }

    const nextQuantity = item.quantity + difference;
    const stock = Number(item.product.quantity) || 0;

    if (nextQuantity <= 0) {
        cart.delete(Number(productId));

    } else if (nextQuantity > stock) {
        showToast("Available stock limit reached.", true);
        return;

    } else {
        item.quantity = nextQuantity;
        cart.set(Number(productId), item);
    }

    renderProducts();
    renderCart();
}


/* CLEAR CART */

function clearCart() {
    if (cart.size === 0) {
        return showToast("Cart is already empty.");
    }

    cart.clear();

    renderProducts();
    renderCart();

    showToast("Cart cleared.");
}


/* DISPLAY CART */

function renderCart() {
    const container = document.getElementById("cartItems");

    const items = [...cart.values()];

    const totalQuantity = items.reduce(
        (sum, item) => sum + item.quantity,
        0
    );

    const subtotal = items.reduce(
        (sum, item) =>
            sum + Number(item.product.price) * item.quantity,
        0
    );

    document.getElementById("cartItemCount").textContent =
        `${totalQuantity} items`;

    document.getElementById("subtotal").textContent =
        money(subtotal);

    document.getElementById("discount").textContent =
        money(0);

    document.getElementById("grandTotal").textContent =
        money(subtotal);

    document.getElementById("checkoutButton").disabled =
        items.length === 0;

    if (items.length === 0) {
        container.innerHTML = `
            <div class="empty-cart">
                <div class="empty-cart-icon">🛒</div>
                <h3>Your cart is empty</h3>
                <p>Add products from the catalogue to start billing.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = items.map(item => {
        const product = item.product;
        const id = Number(product.productId);

        const lineTotal =
            Number(product.price) * item.quantity;

        return `
            <div class="cart-item">

                <div class="cart-item-visual">
                    ${productEmoji(product)}
                </div>

                <div class="cart-item-details">

                    <div class="cart-item-name">
                        ${escapeHtml(product.productName)}
                    </div>

                    <div class="cart-item-price">
                        ${money(product.price)} each
                    </div>

                    <div class="cart-item-bottom">

                        <div class="quantity-control">

                            <button
                                onclick="changeQuantity(${id}, -1)"
                                aria-label="Decrease quantity"
                            >−</button>

                            <span>${item.quantity}</span>

                            <button
                                onclick="changeQuantity(${id}, 1)"
                                aria-label="Increase quantity"
                            >+</button>

                        </div>

                        <strong class="cart-line-total">
                            ${money(lineTotal)}
                        </strong>

                    </div>
                </div>
            </div>
        `;
    }).join("");
}


/* CHECKOUT - MULTIPLE PRODUCTS IN ONE INVOICE */

async function checkout() {
    if (cart.size === 0) {
        showToast("Add products to the cart first.", true);
        return;
    }

    const customerId = Number(
        document.getElementById("customerSelect").value
    );

    if (!customerId) {
        showToast("Please select a customer.", true);
        return;
    }

    const customer = customers.find(
        item => Number(item.customerId) === customerId
    );

    if (!customer) {
        showToast("Selected customer was not found.", true);
        return;
    }

    const button = document.getElementById("checkoutButton");

    if (button.disabled) {
        return;
    }

    const invoiceItems = [...cart.values()].map(item => ({
        productId: Number(item.product.productId),
        productName: item.product.productName,
        quantity: Number(item.quantity),
        unitPrice: Number(item.product.price),
        total:
            Number(item.product.price) * Number(item.quantity)
    }));

    const expectedTotal = invoiceItems.reduce(
        (sum, item) => sum + item.total,
        0
    );

    button.disabled = true;
    button.innerHTML = "<span>Processing sale...</span>";

    let saleCreated = false;

    try {
        /* Check current stock before checkout */

        const latestProducts = await apiRequest(API.products);

        for (const item of invoiceItems) {
            const latestProduct = latestProducts.find(
                product =>
                    Number(product.productId) === item.productId
            );

            if (!latestProduct) {
                throw new Error(
                    `${item.productName} is no longer available.`
                );
            }

            if (Number(latestProduct.quantity) < item.quantity) {
                throw new Error(
                    `Insufficient stock for ${item.productName}.`
                );
            }
        }

        /* Send every cart item in ONE request */

        const result = await apiRequest(API.createMultipleSale, {
            method: "POST",

            body: JSON.stringify({
                customerId: customerId,

                items: invoiceItems.map(item => ({
                    productId: item.productId,
                    quantity: item.quantity
                }))
            })
        });

        const saleId = Number(result?.saleId);

        if (!saleId) {
            throw new Error(
                "Server did not return a valid invoice number."
            );
        }

        saleCreated = true;

        /* Get invoice total from backend if endpoint is available */

        let invoiceTotal = expectedTotal;

        try {
            const backendTotal = await apiRequest(
                `${API.saleItems}/sale/${saleId}/invoice-total`
            );

            const parsedTotal = Number(backendTotal);

            if (Number.isFinite(parsedTotal)) {
                invoiceTotal = parsedTotal;
            }

        } catch (error) {
            /*
             * The sale already exists.
             * Do not repeat checkout if invoice total fails.
             */
        }

        currentInvoice = {
            saleId: saleId,

            customerName:
                customer.customerName || "Customer",

            saleDate: new Date(),

            items: invoiceItems,

            total: invoiceTotal
        };

        renderInvoice(currentInvoice);

        document.getElementById("invoiceModal")
            .classList.add("show");

        /* Clear cart after successful sale */

        cart.clear();

        renderProducts();
        renderCart();

        await Promise.allSettled([
            loadProducts(),
            loadSales()
        ]);

        showToast(
            `Invoice #${saleId} created successfully!`
        );

    } catch (error) {
        showToast(
            saleCreated
                ? `Sale was created, but invoice display had an issue: ${error.message}`
                : (error.message || "Checkout failed."),
            true
        );

    } finally {
        button.innerHTML = `
            <span>Complete checkout</span>
            <span>→</span>
        `;

        button.disabled = cart.size === 0;
    }
}


/* INVOICE DISPLAY */

function renderInvoice(invoice) {
    document.getElementById("invoiceSaleId").textContent =
        `#${invoice.saleId}`;

    document.getElementById("invoiceCustomer").textContent =
        invoice.customerName;

    document.getElementById("invoiceTotal").textContent =
        money(invoice.total);

    const modal = document.getElementById("invoiceModal");

    let printArea = document.getElementById("invoicePrintArea");

    if (!printArea) {
        printArea = document.createElement("div");

        printArea.id = "invoicePrintArea";
        printArea.className = "invoice-print-area";

        const totalElement =
            document.getElementById("invoiceTotal");

        totalElement.parentElement.insertBefore(
            printArea,
            totalElement
        );
    }

    printArea.innerHTML = `
        <div class="invoice-heading">
            <h2>FreshMart Supermarket</h2>
            <p>Thank you for shopping with us!</p>
        </div>

        <div class="invoice-meta">
            <p>
                <strong>Invoice No:</strong>
                #${invoice.saleId}
            </p>

            <p>
                <strong>Customer:</strong>
                ${escapeHtml(invoice.customerName)}
            </p>

            <p>
                <strong>Date:</strong>
                ${formatDate(invoice.saleDate)}
            </p>
        </div>

        <table class="invoice-items-table">

            <thead>
                <tr>
                    <th>Product</th>
                    <th>Qty</th>
                    <th>Unit price</th>
                    <th>Amount</th>
                </tr>
            </thead>

            <tbody>
                ${invoice.items.map(item => `
                    <tr>
                        <td>${escapeHtml(item.productName)}</td>
                        <td>${item.quantity}</td>
                        <td>${money(item.unitPrice)}</td>
                        <td>${money(item.total)}</td>
                    </tr>
                `).join("")}
            </tbody>

            <tfoot>
                <tr>
                    <th colspan="3">Grand Total</th>
                    <th>${money(invoice.total)}</th>
                </tr>
            </tfoot>

        </table>
    `;

    let printButton =
        document.getElementById("printInvoiceButton");

    if (!printButton) {
        printButton = document.createElement("button");

        printButton.id = "printInvoiceButton";
        printButton.type = "button";
        printButton.className = "print-invoice-button";
        printButton.textContent = "Print Invoice";

        printButton.addEventListener("click", printInvoice);

        modal.appendChild(printButton);
    }
}


/* CLOSE INVOICE */

function closeInvoice() {
    document.getElementById("invoiceModal")
        .classList.remove("show");
}


/* PRINT INVOICE */

function printInvoice() {
    const content =
        document.getElementById("invoicePrintArea");

    if (!content || !currentInvoice) {
        showToast("Invoice details are not available.", true);
        return;
    }

    const printWindow = window.open(
        "",
        "_blank",
        "width=850,height=700"
    );

    if (!printWindow) {
        showToast(
            "Allow pop-ups in your browser to print the invoice.",
            true
        );
        return;
    }

    printWindow.document.open();

    printWindow.document.write(`
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">

            <title>
                FreshMart Invoice #${currentInvoice.saleId}
            </title>

            <style>
                body {
                    font-family: Arial, sans-serif;
                    color: #222;
                    padding: 24px;
                }

                .invoice-heading {
                    text-align: center;
                    margin-bottom: 24px;
                }

                .invoice-heading h2 {
                    color: #138747;
                    margin-bottom: 4px;
                }

                .invoice-meta {
                    margin: 20px 0;
                }

                .invoice-meta p {
                    margin: 6px 0;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 16px;
                }

                th, td {
                    border-bottom: 1px solid #ddd;
                    padding: 10px;
                    text-align: left;
                }

                thead th {
                    background: #eef8f1;
                }

                tfoot th {
                    border-top: 2px solid #222;
                    font-size: 1.05rem;
                }

                @media print {
                    body {
                        padding: 0;
                    }
                }
            </style>
        </head>

        <body>
            ${content.innerHTML}
        </body>

        </html>
    `);

    printWindow.document.close();
    printWindow.focus();

    setTimeout(() => {
        printWindow.print();
        printWindow.close();
    }, 300);
}


/* MONEY FORMAT */

function money(value) {
    const amount = Number(value) || 0;

    return amount.toLocaleString("en-IN", {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}


/* DATE FORMAT */

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}


/* HTML ESCAPING */

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}


/* JAVASCRIPT STRING ESCAPING */

function escapeJs(value) {
    return String(value ?? "")
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"');
}


/* TOAST MESSAGE */

function showToast(message, isError = false) {
    const toast = document.getElementById("toast");

    if (!toast) {
        console[isError ? "error" : "log"](message);
        return;
    }

    toast.textContent = message;

    toast.classList.toggle("error", isError);
    toast.classList.add("show");

    clearTimeout(toastTimer);

    toastTimer = setTimeout(() => {
        toast.classList.remove("show");
    }, 3500);
}