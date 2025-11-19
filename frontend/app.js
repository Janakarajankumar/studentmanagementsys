console.log("app.js loaded");

const API_BASE = `http://${window.location.hostname}:8080`;

let currentUser = null;   // { username, name, email, role }
let authHeader = null;    // Basic ...

// ---------- helpers ----------

function isAdminRole(role) {
  return role === "ADMIN" || role === "ROLE_ADMIN";
}

function isCurrentAdmin() {
  return currentUser && isAdminRole(currentUser.role);
}

function showAlert(type, message) {
  const box = document.getElementById("alertBox");
  const msg = document.getElementById("alertMessage");
  box.className = `alert alert-${type}`;
  msg.textContent = message;
  box.classList.remove("d-none");
}

function hideAlert() {
  document.getElementById("alertBox").classList.add("d-none");
}

function setLoginError(message) {
  const el = document.getElementById("login-error");
  if (el) el.textContent = message || "";
}

function clearLoginError() {
  setLoginError("");
}

function applyLoggedInUI() {
  if (!currentUser) return;
  console.log("applyLoggedInUI, role =", currentUser.role);

  document.getElementById("authSection").style.display = "none";
  document.getElementById("mainSection").style.display = "block";

  const userInfo = document.getElementById("user-info");
  const userName = document.getElementById("current-user");
  const logoutBtn = document.getElementById("logout-btn");
  const loginHistoryBtn = document.getElementById("loginHistoryBtn");

  userName.textContent = currentUser.username;
  userInfo.classList.remove("d-none");
  logoutBtn.classList.remove("d-none");

  if (isCurrentAdmin()) {
    loginHistoryBtn.classList.remove("d-none");
  } else {
    loginHistoryBtn.classList.add("d-none");
  }

 const adminEls = document.querySelectorAll(".admin-only");
console.log("admin-only elements found:", adminEls.length);

adminEls.forEach(el => {
  if (isCurrentAdmin()) {
    // show for admin
    if (el.tagName === "TH" || el.tagName === "TD") {
      el.style.display = "table-cell";
    } else {
      el.style.display = "block";
    }
  } else {
    el.style.display = "none";
  }
});

}

function applyLoggedOutUI() {
  document.getElementById("authSection").style.display = "block";
  document.getElementById("mainSection").style.display = "none";

  const userInfo = document.getElementById("user-info");
  const userName = document.getElementById("current-user");
  const logoutBtn = document.getElementById("logout-btn");
  const loginHistoryBtn = document.getElementById("loginHistoryBtn");

  userName.textContent = "";
  userInfo.classList.add("d-none");
  logoutBtn.classList.add("d-none");
  loginHistoryBtn.classList.add("d-none");

  document.querySelectorAll(".admin-only").forEach(el => {
  el.style.display = "none";   // always hide when logged out
});

}

// ---------- DOMContentLoaded ----------

document.addEventListener("DOMContentLoaded", () => {
  console.log("DOM ready");
  applyLoggedOutUI();
  hideAlert();
  clearLoginError();

  document.getElementById("alertCloseBtn").addEventListener("click", hideAlert);

  document.getElementById("loginForm").addEventListener("submit", handleLogin);
  document.getElementById("signupForm").addEventListener("submit", handleSignup);
  document.getElementById("logout-btn").addEventListener("click", handleLogout);
  document.getElementById("loginHistoryBtn").addEventListener("click", showLoginHistory);

  document.getElementById("studentSearch").addEventListener("input", filterStudents);

  const addBtn = document.getElementById("addStudentBtn");
  if (addBtn) {
    addBtn.addEventListener("click", () => {
  if (!isCurrentAdmin()) {
    showAlert("danger", "Only admin can add students.");
    return;
  }
  window.location.href = "student_form.html?mode=add";
});

  } else {
    console.warn("addStudentBtn not found!");
  }
});

// ---------- AUTH ----------

async function handleLogin(event) {
  event.preventDefault();
  hideAlert();
  clearLoginError();

  const loginId = document.getElementById("loginUsername").value.trim();
  const password = document.getElementById("loginPassword").value;

  if (!loginId || !password) {
    setLoginError("Please enter username/email and password.");
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ loginId, password })
    });

    if (!response.ok) {
      let data = {};
      try { data = await response.json(); } catch (e) {}

      if (response.status === 404 && data.error === "NO_USER") {
        setLoginError("No user found with that username/email.");
      } else if (response.status === 401 && data.error === "WRONG_PASSWORD") {
        setLoginError("Wrong password.");
      } else {
        setLoginError("Login failed (" + response.status + ").");
      }
      return;
    }

    const data = await response.json();  // { username, name, email, role }

    // normalise role so we always treat admin correctly
    const role = isAdminRole(data.role) ? "ADMIN" : data.role;

    currentUser = {
      username: data.username,
      name: data.name,
      email: data.email,
      role: role
    };

    authHeader = "Basic " + btoa(data.username + ":" + password);
    sessionStorage.setItem("authHeader", authHeader);
    sessionStorage.setItem("currentUserRole", currentUser.role);

    applyLoggedInUI();
    showAlert("success", "Login successful.");
    await loadDashboard();

  } catch (err) {
    console.error(err);
    setLoginError("Error connecting to server.");
  }
}

async function handleSignup(e) {
  e.preventDefault();
  hideAlert();
  clearLoginError();

  const name = document.getElementById("signupName").value.trim();
  const email = document.getElementById("signupEmail").value.trim();
  const password = document.getElementById("signupPassword").value;

  if (!name || !email || !password) {
    showAlert("danger", "Please fill all fields.");
    return;
  }

  try {
    const resp = await fetch(`${API_BASE}/api/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, email, password })
    });

    if (!resp.ok) {
      let data = {};
      try { data = await resp.json(); } catch (e) {}
      if (resp.status === 409 && data.error === "EMAIL_EXISTS") {
        showAlert("danger", "Email already exists. Try another.");
      } else {
        showAlert("danger", `Signup failed (${resp.status}).`);
      }
      return;
    }

    showAlert("success", "Account created! You can now login.");
    const loginTab = document.querySelector("#login-tab");
    const tab = new bootstrap.Tab(loginTab);
    tab.show();

  } catch (err) {
    console.error(err);
    showAlert("danger", "Error connecting to server.");
  }
}

function handleLogout() {
  currentUser = null;
  authHeader = null;
  sessionStorage.removeItem("authHeader");
  sessionStorage.removeItem("currentUserRole");
  hideAlert();
  clearLoginError();

  document.getElementById("loginPassword").value = "";
  document.getElementById("signupPassword").value = "";

  applyLoggedOutUI();
}

// ---------- DASHBOARD ----------

let allStudents = [];

async function loadDashboard() {
  await Promise.all([
    loadStudents(),
    loadAdminLogins()
  ]);
}

async function loadStudents() {
  if (!authHeader) return;

  try {
    const resp = await fetch(`${API_BASE}/api/students`, {
      headers: { "Authorization": authHeader }
    });

    if (!resp.ok) {
      console.error("students error", resp.status);
      return;
    }

    const students = await resp.json();
    allStudents = students;
    renderStudents(students);

    document.getElementById("totalStudents").textContent = students.length.toString();
  } catch (err) {
    console.error(err);
  }
}

async function loadAdminLogins() {
  if (!authHeader || !isCurrentAdmin()) return;

  try {
    const resp = await fetch(`${API_BASE}/api/admin/logins`, {
      headers: { "Authorization": authHeader }
    });

    if (!resp.ok) {
      console.error("admin/logins error", resp.status);
      return;
    }
    const logins = await resp.json();
    const el = document.getElementById("totalLogins");
    if (el) el.textContent = logins.length.toString();
  } catch (err) {
    console.error(err);
  }
}

function renderStudents(students) {
  const tbody = document.getElementById("studentsTableBody");
  tbody.innerHTML = "";

  students.forEach(stu => {
    const tr = document.createElement("tr");

    tr.innerHTML = `
      <td>${stu.id}</td>
      <td>
        <button class="btn btn-link p-0 student-name-btn" data-id="${stu.id}">
          ${stu.name ?? ""}
        </button>
      </td>
      <td>${stu.email ?? ""}</td>
      <td>
        <button class="btn btn-sm btn-outline-secondary" data-id="${stu.id}" data-action="exams">
          View
        </button>
      </td>
      <td>
        <button class="btn btn-sm btn-outline-secondary" data-id="${stu.id}" data-action="fees">
          View
        </button>
      </td>
    <td class="admin-only">
  <button class="btn btn-sm btn-outline-primary" data-id="${stu.id}" data-action="edit">
    Edit
  </button>
  <button class="btn btn-sm btn-outline-danger ms-1" data-id="${stu.id}" data-action="delete">
    Delete
  </button>
</td>

    `;
    tbody.appendChild(tr);
  });

 document.querySelectorAll(".admin-only").forEach(el => {
  if (isCurrentAdmin()) {
    if (el.tagName === "TH" || el.tagName === "TD") {
      el.style.display = "table-cell";
    } else {
      el.style.display = "block";
    }
  } else {
    el.style.display = "none";
  }
});


  tbody.querySelectorAll("button[data-action]").forEach(btn => {
    btn.addEventListener("click", handleStudentAction);
  });

  tbody.querySelectorAll(".student-name-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-id");
      const student = allStudents.find(s => s.id == id);
      if (student) showStudentDetails(student);
    });
  });
}

function filterStudents() {
  const q = document.getElementById("studentSearch").value.toLowerCase();
  const filtered = allStudents.filter(s =>
    (s.name && s.name.toLowerCase().includes(q)) ||
    (s.email && s.email.toLowerCase().includes(q))
  );
  renderStudents(filtered);
}

async function handleStudentAction(e) {
  const id = e.target.getAttribute("data-id");
  const action = e.target.getAttribute("data-action");

  const student = allStudents.find(s => s.id == id);
  if (!student) return;

  if (action === "exams") {
    showExams(student);
    return;
  }

  if (action === "fees") {
    await showFees(student);
    return;
  }

  // below: admin-only
  if (!isCurrentAdmin()) {
    showAlert("danger", "Only admin can modify student data.");
    return;
  }

  if (action === "edit") {
  if (!isCurrentAdmin()) {
    showAlert("danger", "Only admin can edit students.");
    return;
  }
  window.location.href = `student_form.html?mode=edit&id=${id}`;
} else if (action === "delete") {
  if (!confirm(`Delete student ${student.name}?`)) return;
  await deleteStudent(id);
}

}

// ---------- Student CRUD ----------

async function createStudent(stu) {
  try {
    const resp = await fetch(`${API_BASE}/api/students`, {
      method: "POST",
      headers: {
        "Authorization": authHeader,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(stu)
    });
    if (!resp.ok) {
      showAlert("danger", `Failed to create student (${resp.status}).`);
      return;
    }
    showAlert("success", "Student created.");
    await loadStudents();
  } catch (err) {
    console.error(err);
  }
}

async function updateStudent(id, stu) {
  try {
    const resp = await fetch(`${API_BASE}/api/students/${id}`, {
      method: "PUT",
      headers: {
        "Authorization": authHeader,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(stu)
    });
    if (!resp.ok) {
      showAlert("danger", `Failed to update student (${resp.status}).`);
      return;
    }
    showAlert("success", "Student updated.");
    await loadStudents();
  } catch (err) {
    console.error(err);
  }
}

async function deleteStudent(id) {
  try {
    const resp = await fetch(`${API_BASE}/api/students/${id}`, {
      method: "DELETE",
      headers: { "Authorization": authHeader }
    });

    console.log("DELETE /api/students/" + id, resp.status);

    if (!resp.ok) {
      showAlert("danger", `Failed to delete student (status ${resp.status}).`);
      return;
    }

    showAlert("success", "Student deleted.");
    await loadStudents();
  } catch (err) {
    console.error("deleteStudent error", err);
    showAlert("danger", "Error while deleting student.");
  }
}


async function showFees(student) {
  try {
    const resp = await fetch(`${API_BASE}/api/students/${student.id}/fees`, {
      headers: { "Authorization": authHeader }
    });
    if (!resp.ok) { alert("Failed to load fees"); return; }
    const fees = await resp.json();
    if (!fees.length) {
      alert(`No fee records for ${student.name}`);
      return;
    }
    let msg = `Fees for ${student.name}:\n\n`;
    fees.forEach(f => {
      msg += `${f.term}: ₹${f.amount} - ${f.paid ? "Paid" : "Pending"} (Due: ${f.dueDate})\n`;
    });
    alert(msg);
  } catch (err) {
    console.error(err);
  }
}

async function showStudentDetails(student) {
  try {
    const [exResp, feeResp] = await Promise.all([
      fetch(`${API_BASE}/api/students/${student.id}/exams`, {
        headers: { "Authorization": authHeader }
      }),
      fetch(`${API_BASE}/api/students/${student.id}/fees`, {
        headers: { "Authorization": authHeader }
      })
    ]);

    let message = `Details for ${student.name} (ID: ${student.id})\nEmail: ${student.email}\n\n`;

    if (exResp.ok) {
      const exams = await exResp.json();
      message += "Exams / Marks:\n";
      if (!exams.length) {
        message += "  No exams.\n";
      } else {
        exams.forEach(ex => {
          message += `  ${ex.semester ?? ""} ${ex.subject}: ${ex.marksObtained}/${ex.maxMarks} on ${ex.examDate}\n`;
        });
      }
      message += "\n";
    }

    if (feeResp.ok) {
      const fees = await feeResp.json();
      message += "Fees:\n";
      if (!fees.length) {
        message += "  No fee records.\n";
      } else {
        fees.forEach(f => {
          message += `  ${f.term}: ₹${f.amount} - ${f.paid ? "Paid" : "Pending"} (Due: ${f.dueDate})\n`;
        });
      }
    }

    alert(message);

  } catch (err) {
    console.error(err);
  }
}

// ---------- Login History ----------

async function showLoginHistory() {
  if (!isCurrentAdmin()) {
    showAlert("danger", "Only admin can view login history.");
    return;
  }

  try {
    const resp = await fetch(`${API_BASE}/api/admin/logins`, {
      headers: { "Authorization": authHeader }
    });

    if (!resp.ok) {
      alert("Failed to load login history (" + resp.status + ")");
      return;
    }

    const logins = await resp.json();
    if (!logins.length) {
      alert("No logins recorded yet.");
      return;
    }

    let msg = "Login History:\n\n";
    logins.forEach(l => {
      const dt = new Date(l.loginTime);
      const when = isNaN(dt.getTime()) ? l.loginTime : dt.toLocaleString();
      msg += `${l.username}  -  ${when}\n`;
    });
    alert(msg);

  } catch (err) {
    console.error(err);
  }
}
