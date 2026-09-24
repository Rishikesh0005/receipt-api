(function () {
  "use strict";

  const BASE_URL = window.location.origin;
  document.getElementById("baseUrlLabel").textContent = BASE_URL;

  // ---------------------------------------------------------------------
  // Fixture scenarios (embedded so the UI works with zero server file deps)
  // ---------------------------------------------------------------------
  const SCENARIOS = [
    {
      key: "clean",
      title: "Cafe Mitte — Clean receipt",
      desc: "Items + VAT reconcile exactly",
      icon: "complete",
      iconChar: "\u2713",
      filename: "receipt-clean.txt",
      text:
        "MERCHANT: Cafe Mitte\n" +
        "DATE: 2026-03-12\n" +
        "CURRENCY: EUR\n\n" +
        "Espresso                    3.50\n" +
        "Sandwich                    8.90\n" +
        "Mineral water               2.60\n\n" +
        "Subtotal                   15.00\n" +
        "VAT 19%                     2.85\n" +
        "TOTAL                      17.85\n",
    },
    {
      key: "taxonly",
      title: "Berlin Taxi GmbH — Tax only",
      desc: "No itemized list, tax-only receipt",
      icon: "failed",
      iconChar: "!",
      filename: "receipt-tax-only.txt",
      text:
        "MERCHANT: Berlin Taxi GmbH\n" +
        "DATE: 2026-03-12\n" +
        "CURRENCY: EUR\n\n" +
        "Trip fare\n" +
        "TOTAL                      24.00\n" +
        "incl. VAT 19%               3.83\n\n" +
        "(No itemized list)\n",
    },
    {
      key: "mismatch",
      title: "Hotel Shop — Mismatch",
      desc: "Line items do not sum to total",
      icon: "review",
      iconChar: "\u26A0",
      filename: "receipt-mismatch.txt",
      text:
        "MERCHANT: Hotel Shop\n" +
        "DATE: 2026-03-13\n" +
        "CURRENCY: EUR\n\n" +
        "Water                       4.00\n" +
        "Snacks                      6.00\n\n" +
        "Subtotal                   10.00\n" +
        "VAT 19%                     1.90\n" +
        "TOTAL                      18.50\n\n" +
        "(Line items do not sum to total)\n",
    },
  ];

  let selectedScenario = SCENARIOS[0];
  let usingCustomText = false;
  let uploadedFile = null;

  let currentReceiptId = null;
  let currentTransactionId = null;
  let currentTransaction = null;

  // ---------------------------------------------------------------------
  // DOM refs
  // ---------------------------------------------------------------------
  const scenarioListEl = document.getElementById("scenarioList");
  const customTextEl = document.getElementById("customText");
  const clearCustomBtn = document.getElementById("clearCustomBtn");
  const dropzoneEl = document.getElementById("dropzone");
  const fileInputEl = document.getElementById("fileInput");
  const dropzoneFileEl = document.getElementById("dropzoneFile");
  const curlListEl = document.getElementById("curlList");
  const runBtn = document.getElementById("runBtn");
  const stepListEl = document.getElementById("stepList");
  const statusPillEl = document.getElementById("statusPill");
  const resultGridEl = document.getElementById("resultGrid");
  const itemsEditorEl = document.getElementById("itemsEditor");
  const addItemBtn = document.getElementById("addItemBtn");
  const patchBtn = document.getElementById("patchBtn");
  const jsonOutputEl = document.getElementById("jsonOutput");
  const httpCodeEl = document.getElementById("httpCode");
  const healthBadge = document.getElementById("healthBadge");
  const healthText = document.getElementById("healthText");

  // ---------------------------------------------------------------------
  // Health check
  // ---------------------------------------------------------------------
  async function checkHealth() {
    try {
      const res = await fetch(BASE_URL + "/health");
      if (res.ok) {
        healthBadge.className = "health ok";
        healthText.textContent = "Server online";
      } else {
        throw new Error("bad status");
      }
    } catch (e) {
      healthBadge.className = "health down";
      healthText.textContent = "Server unreachable";
    }
  }
  checkHealth();
  setInterval(checkHealth, 15000);

  // ---------------------------------------------------------------------
  // cURL command reference — always reflects the current base URL and
  // the most recent receipt/transaction ids, so it works like a live
  // Postman collection you can copy straight into a terminal.
  // ---------------------------------------------------------------------
  function curlCommands() {
    const rid = currentReceiptId || "{receiptId}";
    const tid = currentTransactionId || "{transactionId}";
    return [
      {
        method: "GET",
        title: "Health check",
        cmd: "curl -s " + BASE_URL + "/health",
      },
      {
        method: "POST",
        title: "Upload a receipt file",
        cmd: 'curl -s -X POST -F "file=@/path/to/receipt.txt" ' + BASE_URL + "/receipts",
      },
      {
        method: "POST",
        title: "Process receipt (OCR + extract)",
        cmd: "curl -s -X POST " + BASE_URL + "/receipts/" + rid + "/process",
      },
      {
        method: "GET",
        title: "Get transaction",
        cmd: "curl -s " + BASE_URL + "/transactions/" + tid,
      },
      {
        method: "POST",
        title: "Re-itemize from stored OCR",
        cmd: "curl -s -X POST " + BASE_URL + "/transactions/" + tid + "/itemize",
      },
      {
        method: "PATCH",
        title: "Update line items (edit/merge/split)",
        cmd:
          "curl -s -X PATCH -H \"Content-Type: application/json\" -d '" +
          JSON.stringify({ items: [{ description: "Coffee & snack", amount: 6.35 }] }) +
          "' " + BASE_URL + "/transactions/" + tid + "/items",
      },
    ];
  }

  function renderCurlCommands() {
    curlListEl.innerHTML = "";
    curlCommands().forEach((c) => {
      const item = document.createElement("div");
      item.className = "curl-item";
      item.innerHTML =
        '<div class="curl-item-head">' +
        '<div class="curl-item-title"><span class="method-badge ' + c.method + '">' + c.method + "</span>" + c.title + "</div>" +
        '<button class="copy-btn" type="button">Copy</button>' +
        "</div>" +
        "<pre>" + escapeHtml(c.cmd) + "</pre>";
      item.querySelector(".copy-btn").addEventListener("click", (e) => {
        navigator.clipboard.writeText(c.cmd).then(() => {
          const btn = e.currentTarget;
          btn.textContent = "Copied!";
          btn.classList.add("copied");
          setTimeout(() => {
            btn.textContent = "Copy";
            btn.classList.remove("copied");
          }, 1500);
        });
      });
      curlListEl.appendChild(item);
    });
  }
  renderCurlCommands();

  // ---------------------------------------------------------------------
  // Scenario list rendering
  // ---------------------------------------------------------------------
  function renderScenarios() {
    scenarioListEl.innerHTML = "";
    SCENARIOS.forEach((s) => {
      const isActive = !usingCustomText && !uploadedFile && s.key === selectedScenario.key;
      const card = document.createElement("div");
      card.className = "scenario-card" + (isActive ? " active" : "");
      card.innerHTML =
        '<div class="scenario-icon ' + s.icon + '">' + s.iconChar + "</div>" +
        '<div class="scenario-body"><strong>' + s.title + "</strong><span>" + s.desc + "</span></div>";
      card.addEventListener("click", () => {
        selectedScenario = s;
        usingCustomText = false;
        uploadedFile = null;
        customTextEl.value = "";
        dropzoneFileEl.textContent = "";
        renderScenarios();
      });
      scenarioListEl.appendChild(card);
    });
  }
  renderScenarios();

  customTextEl.addEventListener("input", () => {
    usingCustomText = customTextEl.value.trim().length > 0;
    if (usingCustomText) {
      uploadedFile = null;
      dropzoneFileEl.textContent = "";
    }
    renderScenarios();
  });

  clearCustomBtn.addEventListener("click", () => {
    customTextEl.value = "";
    usingCustomText = false;
    renderScenarios();
  });

  // ---------------------------------------------------------------------
  // Real file upload: drag & drop + click-to-browse.
  // A file selected here takes priority over scenarios/custom text.
  // ---------------------------------------------------------------------
  function setUploadedFile(file) {
    if (!file) return;
    uploadedFile = file;
    usingCustomText = false;
    customTextEl.value = "";
    dropzoneFileEl.textContent = file.name + " (" + Math.max(1, Math.round(file.size / 1024)) + " KB)";
    renderScenarios();
  }

  dropzoneEl.addEventListener("click", () => fileInputEl.click());

  fileInputEl.addEventListener("change", (e) => {
    setUploadedFile(e.target.files && e.target.files[0]);
  });

  ["dragenter", "dragover"].forEach((evt) =>
    dropzoneEl.addEventListener(evt, (e) => {
      e.preventDefault();
      dropzoneEl.classList.add("drag-over");
    })
  );
  ["dragleave", "drop"].forEach((evt) =>
    dropzoneEl.addEventListener(evt, (e) => {
      e.preventDefault();
      dropzoneEl.classList.remove("drag-over");
    })
  );
  dropzoneEl.addEventListener("drop", (e) => {
    const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
    setUploadedFile(file);
  });

  // ---------------------------------------------------------------------
  // Pipeline step UI helpers
  // ---------------------------------------------------------------------
  function resetSteps() {
    stepListEl.querySelectorAll("li").forEach((li) => {
      li.className = "";
      li.querySelector(".step-status").textContent = "Idle";
    });
  }

  function setStep(name, state, label) {
    const li = stepListEl.querySelector('li[data-step="' + name + '"]');
    if (!li) return;
    li.className = state;
    li.querySelector(".step-status").textContent = label;
  }

  // ---------------------------------------------------------------------
  // Result rendering
  // ---------------------------------------------------------------------
  function renderTransaction(tx) {
    currentTransaction = tx;
    statusPillEl.textContent = tx.itemizeStatus;
    statusPillEl.className = "status-pill " + tx.itemizeStatus;

    const itemSum = (tx.items || []).reduce((s, i) => s + Number(i.amount || 0), 0);
    const taxSum = (tx.taxes || []).reduce((s, t) => s + Number(t.amount || 0), 0);
    const actual = itemSum + taxSum;
    const diff = Math.abs(actual - Number(tx.total));
    const reconciled = diff <= 0.02;

    let html = "";
    html += row("Merchant", tx.merchant);
    html += row("Date", tx.date);
    html += row("Currency", tx.currency);
    html += row("Grand total", fmt(tx.total) + " " + tx.currency);

    html += '<div class="subblock-title">Taxes</div>';
    if (!tx.taxes || tx.taxes.length === 0) {
      html += '<div class="line-row"><span>No tax lines</span></div>';
    } else {
      tx.taxes.forEach((t) => {
        html += '<div class="line-row"><span>' + t.name + (t.rate != null ? " (" + t.rate + "%)" : "") + '</span><span class="amt">' + fmt(t.amount) + "</span></div>";
      });
    }

    html += '<div class="subblock-title">Line items (' + (tx.items ? tx.items.length : 0) + ")</div>";
    if (!tx.items || tx.items.length === 0) {
      html += '<div class="line-row"><span>No items extracted</span></div>';
    } else {
      tx.items.forEach((i) => {
        html += '<div class="line-row"><span>' + escapeHtml(i.description) + '</span><span class="amt">' + fmt(i.amount) + "</span></div>";
      });
    }

    html += '<div class="reconcile-bar ' + (reconciled ? "ok" : "bad") + '">' +
      "<span>Items + tax = " + fmt(actual) + "</span>" +
      "<span>" + (reconciled ? "\u2713 reconciles" : "\u2717 off by " + fmt(diff)) + "</span>" +
      "</div>";

    resultGridEl.innerHTML = html;
    renderItemsEditor(tx);
  }

  function row(k, v) {
    return '<div class="field-row"><span class="k">' + k + '</span><span class="v">' + escapeHtml(String(v)) + "</span></div>";
  }

  function fmt(n) {
    const num = Number(n);
    return isNaN(num) ? "-" : num.toFixed(2);
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
  }

  // ---------------------------------------------------------------------
  // Items editor (for PATCH)
  // ---------------------------------------------------------------------
  function renderItemsEditor(tx) {
    itemsEditorEl.innerHTML = "";
    (tx.items || []).forEach((item) => addItemRow(item.description, item.amount));
    if (!tx.items || tx.items.length === 0) {
      addItemRow("", "");
    }
  }

  function addItemRow(description, amount) {
    const row = document.createElement("div");
    row.className = "item-row";
    row.innerHTML =
      '<input type="text" class="desc-input" placeholder="Description" value="' + escapeHtml(description || "") + '" />' +
      '<input type="number" step="0.01" class="amt-input" placeholder="0.00" value="' + (amount != null ? amount : "") + '" />' +
      '<button class="remove-btn" type="button">&times;</button>';
    row.querySelector(".remove-btn").addEventListener("click", () => row.remove());
    itemsEditorEl.appendChild(row);
  }

  addItemBtn.addEventListener("click", () => addItemRow("", ""));

  // ---------------------------------------------------------------------
  // API calls
  // ---------------------------------------------------------------------
  function currentReceiptBlob() {
    if (uploadedFile) {
      return { blob: uploadedFile, filename: uploadedFile.name };
    }
    const text = usingCustomText ? customTextEl.value : selectedScenario.text;
    const filename = usingCustomText ? "custom-receipt.txt" : selectedScenario.filename;
    return { blob: new Blob([text], { type: "text/plain" }), filename };
  }

  async function uploadReceipt() {
    const { blob, filename } = currentReceiptBlob();
    const form = new FormData();
    form.append("file", blob, filename);
    const res = await fetch(BASE_URL + "/receipts", { method: "POST", body: form });
    const json = await res.json();
    if (!res.ok) throw new ApiError(res.status, json);
    return json;
  }

  async function processReceipt(receiptId) {
    const res = await fetch(BASE_URL + "/receipts/" + receiptId + "/process", { method: "POST" });
    const json = await res.json();
    if (!res.ok) throw new ApiError(res.status, json);
    return json;
  }

  async function getTransaction(transactionId) {
    const res = await fetch(BASE_URL + "/transactions/" + transactionId);
    const json = await res.json();
    if (!res.ok) throw new ApiError(res.status, json);
    return json;
  }

  async function reItemize(transactionId) {
    const res = await fetch(BASE_URL + "/transactions/" + transactionId + "/itemize", { method: "POST" });
    const json = await res.json();
    if (!res.ok) throw new ApiError(res.status, json);
    return json;
  }

  async function patchItems(transactionId, items) {
    const res = await fetch(BASE_URL + "/transactions/" + transactionId + "/items", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ items }),
    });
    const json = await res.json();
    return { ok: res.ok, status: res.status, json };
  }

  function ApiError(status, body) {
    this.status = status;
    this.body = body;
  }

  function showJson(status, body) {
    httpCodeEl.textContent = "HTTP " + status;
    httpCodeEl.className = "http-code " + (status < 300 ? "ok2xx" : status < 500 ? "err4xx" : "err5xx");
    jsonOutputEl.textContent = JSON.stringify(body, null, 2);
  }

  // ---------------------------------------------------------------------
  // Full workflow runner
  // ---------------------------------------------------------------------
  runBtn.addEventListener("click", async () => {
    resetSteps();
    runBtn.disabled = true;
    resultGridEl.innerHTML = '<div class="empty-state">Running&hellip;</div>';

    try {
      setStep("upload", "active", "Uploading&hellip;");
      const uploadRes = await uploadReceipt();
      currentReceiptId = uploadRes.receiptId;
      setStep("upload", "done", "Uploaded");
      showJson(201, uploadRes);
      renderCurlCommands();

      setStep("process", "active", "Processing&hellip;");
      const processRes = await processReceipt(currentReceiptId);
      currentTransactionId = processRes.transactionId;
      setStep("process", "done", processRes.status);
      showJson(200, processRes);
      renderCurlCommands();

      setStep("get", "active", "Fetching&hellip;");
      const tx1 = await getTransaction(currentTransactionId);
      setStep("get", "done", "Loaded");
      renderTransaction(tx1);
      showJson(200, tx1);

      setStep("itemize", "active", "Re-itemizing&hellip;");
      const tx2 = await reItemize(currentTransactionId);
      setStep("itemize", "done", tx2.itemizeStatus);
      renderTransaction(tx2);
      showJson(200, tx2);
    } catch (err) {
      const status = err.status || "?";
      const body = err.body || { message: String(err) };
      const failingStep = ["upload", "process", "get", "itemize"].find(
        (name) => stepListEl.querySelector('li[data-step="' + name + '"]').className === "active"
      );
      if (failingStep) setStep(failingStep, "error", "HTTP " + status);
      showJson(status, body);
    } finally {
      runBtn.disabled = false;
    }
  });

  // ---------------------------------------------------------------------
  // PATCH tester
  // ---------------------------------------------------------------------
  patchBtn.addEventListener("click", async () => {
    if (!currentTransactionId) {
      showJson(400, { code: "NO_TRANSACTION", message: "Run the workflow first to create a transaction." });
      return;
    }

    const items = Array.from(itemsEditorEl.querySelectorAll(".item-row")).map((row) => ({
      description: row.querySelector(".desc-input").value || "Item",
      amount: parseFloat(row.querySelector(".amt-input").value) || 0,
    }));

    patchBtn.disabled = true;
    try {
      const { ok, status, json } = await patchItems(currentTransactionId, items);
      showJson(status, json);
      if (ok) {
        renderTransaction(json);
      }
    } catch (err) {
      showJson(500, { message: String(err) });
    } finally {
      patchBtn.disabled = false;
    }
  });
})();
