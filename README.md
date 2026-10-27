<div align="center">

<img src="docs/images/logo.png" alt="Budgie app icon" width="160"/>

# Budgie

**Budgeting that's simple, visual and personal.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Backend](https://img.shields.io/badge/backend-Firebase-FFCA28?logo=firebase&logoColor=black)
<!-- TODO: replace OWNER/REPO with your repo path so the build badge works -->
![Build](https://github.com/Fate2664/BudgieApp_OSC/actions/workflows/build.yml/badge.svg)

</div>

---

## Table of Contents

1. [About Budgie](#1-about-budgie)
2. [Purpose](#2-purpose)
3. [Features](#3-features)
4. [Design Considerations](#4-design-considerations)
5. [Tech Stack](#5-tech-stack)
6. [Using GitHub and GitHub Actions](#6-using-github-and-github-actions)
7. [Getting Started](#7-getting-started)
8. [Project Planning](#8-project-planning)
9. [Team](#9-team)
10. [References](#10-references)

---

## 1. About Budgie

Budgie is an Android budgeting app built for the OPSC7311 module at IIE Vega School. The name plays on the word "budget" and on our budgerigar mascot, who appears throughout the app.

Many budgeting apps overwhelm people with detail. Budgie takes the opposite approach: a clean interface, graphs and widgets that explain your finances at a glance, and an envelope-style system that helps you stay disciplined with your money.

**Explanation Video:** (https://drive.google.com/file/d/1DqNrZjU0joTLe8lqwvnjoWJiYBAQyLBK/view?usp=sharing)

---

## 2. Purpose

Budgie was created to:

- Give people a **simple, intuitive** way to track income and expenses.
- Help users **control spending** using budgets and per-category spending limits.
- Make spending patterns **easy to understand** through graphs instead of spreadsheets.
- Offer **personalised guidance** through an AI assistant that understands the user's own budget.
- Keep user data **secure** and available across devices.

> Research phase: we studied Wallet, Goodbudget and Vault22 to see what users value and where existing apps fall short. Those findings shaped Budgie's feature set. See the [References](#10-references).

---

## 3. Features

| Feature | Description |
|---|---|
| **Logins / Accounts** | Register and log in with your own account so your financial data stays private to you. |
| **Firebase Database** | Cloud storage of user data so it is tied to your account and accessible across devices. |
| **Time Period Budgeting** | Set a budget for a chosen time period (e.g. monthly) and track spending against it. |
| **Category Spending Limits** | Set a limit for each category and get a clear indication when you go over. |
| **Expense and Income History** | Browse past transactions in a chronological list with amount, date, description and category. |
| **Budget Progress Dashboard** | See total budget, amount spent and amount remaining, with progress bars per category. |
| **Cash Flow Graph** | Compare income against expenditure over a selected period to spot trends. |
| **Expense Pie Chart** | See at a glance how your spending is split between categories. |
| **Goal System** | Set and work towards budgeting goals that encourage good financial habits. |
| **Gamification Features** | These gamified features are intended to enhance the user experience and prompt further engagement. |
| **AI Budgeting Assistant (LLM)** | A chat assistant that uses your own budget data to give personalised recommendations. |

### Screenshots

| Login | Dashboard | Budgets |
|:---:|:---:|:---:|
| <img src="docs/images/login.png" width="200"/> | <img src="docs/images/dashboard.png" width="200"/> | <img src="docs/images/budgets.png" width="200"/> |

| Transactions | Cash Flow | AI Assistant |
|:---:|:---:|:---:|
| <img src="docs/images/transactions.png" width="200"/> | <img src="docs/images/dashboard-2.png" width="200"/> | <img src="docs/images/ai-assistant.png" width="200"/> |

---

## 4. Design Considerations

### 4.1 Simplicity first
The interface prioritises clarity and ease of navigation. Information is shown in widgets that use either graphs or short numbers, with brief descriptions so users understand their data at a glance.

### 4.2 Consistent navigation
A persistent header holds navigation to **Accounts**, **Budgets & Goals** and the **AI Chatbot** and **Income & Expense* tracking can be accessed using the plus on the bottom-right side of the user's screen.

### 4.3 Visual feedback
Progress bars switch between green and red depending on whether the user is under or over their limit, so overspending is obvious without reading numbers.

### 4.4 Category-style budgeting
Funds are allocated to categories such as Groceries, Bills and Transport, users can make their own custom categories too. This encourages spending discipline by making each category's limit visible.

### 4.5 Financial Goals
These goals are set by users in order to save for a large scale future purchase or investment, users can also set a target date for this financial goal.

### 4.6 Personalised AI, not a generic chatbot
The assistant has context about the user's budget, limits and spending trends, so its answers are tailored. Responses are framed as recommendations rather than guaranteed financial advice.

### 4.7 Security and privacy
Users can only access their own financial data. The AI assistant is limited to the authenticated user's information.

### 4.8 Design process
We began with mock-ups of every screen and a navigation flow before writing any code.

| Planned (mock-up) | Final (app) |
|:---:|:---:|
| <img src="docs/images/mockup-login.png" width="220"/> | <img src="docs/images/login.png" width="220"/> |
| <img src="docs/images/mockup-home.png" width="220"/> | <img src="docs/images/dashboard.png" width="220"/> |
| <img src="docs/images/mockup-graphs.png" width="220"/> | <img src="docs/images/dashboard-2.png" width="220"/> |

---

## 5. Tech Stack

| Area | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| IDE | Android Studio |
| Authentication | Firebase Authentication |
| Database | Firebase (Cloud Firestore) |
| AI Assistant | LLM (Ollama, llama 3.2) |
| CI/CD | GitHub Actions |

---

## 6. Using GitHub and GitHub Actions

### 6.1 How we used GitHub

- **Version control:** all code lives in this repository so every change is tracked.
- **Branching:** Our most recent working build would remain on the main branch and other teammates would make use of other branches for adding new features.
- **Commits:** Commits involved the main topic of the commit (what feature it was for) and the progress of it at the time of the commit.
- **Pull requests / code review:** Before merging to main the branch code would be tested to ensure its stability.

### 6.2 How we used GitHub Actions

GitHub Actions automates checks every time code is pushed, so problems are caught early instead of at hand-in time.

**Workflow file:** [`.github/workflows/build.yml`](.github/workflows/build.yml)

| Step | What it does |
|---|---|
| **Trigger** | Runs on every push and pull request to `master`, and can be started manually with the "Run workflow" button. |
| **Check out code** | Downloads the repository onto a fresh Ubuntu virtual machine provided by GitHub. |
| **Set up JDK 17** | Installs the Java version needed to build the Android project. |
| **Set up Gradle** | Prepares Gradle and caches dependencies so later runs are faster. |
| **Make `gradlew` executable** | Gives the Gradle wrapper permission to run on Linux. |
| **Build debug APK** | Runs `./gradlew assembleDebug` to compile the app. If this step fails, the run is marked as failed. |

**Why this matters:** This feature would be very useful had we used it from the start. In simple terms every time one of us pushes code to `master`, this feature automatically picks up the latest version of the project and tries to build the app, just as we would in Android Studio. It then reports back with a pass or fail. This would've saved a lot more time for programming.

**What happens when we push code:**

1. A team member pushes a commit to `master`.
2. GitHub sees the push and starts the workflow automatically, with no one having to click anything.
3. GitHub starts a brand new, empty computer in the cloud.
4. The workflow downloads our code onto that computer and installs Java and Gradle, the tools needed to build an Android app.
5. It builds the app (`./gradlew assembleDebug`).
6. If the build succeeds, the run shows a green tick and the badge at the top of this README stays green. If something is wrong, the run shows a red cross and the log shows exactly which step failed and why.

**Why a fresh computer matters:** An app can work on one person's laptop only because of something installed or saved on that machine, such as a leftover file or a different tool version. Because the workflow starts from nothing every time, a green result tells us the project builds from the code in the repository alone. This tell us what we would need to get it running.

---

## 7. Getting Started

### Prerequisites
- Android Studio Version: Android Studio Quail 2 | 2026.1.2
- Android device or emulator: min SDK 34
- A Firebase project <!--(TODO or explain how to use ours)-->

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/Fate2664/BudgieApp_OSC

# 2. Open the project in Android Studio

# 3. Add your google-services.json to the app/ folder

# 4. Sync Gradle and run the app
```

---

## 8. Project Planning

Our project followed a plan covering research, design, development and refinement.

<img src="docs/images/gantt-chart.png" alt="Project timeline" width="700"/>

---

## 9. Team

| Name | Student Number |
|---|---|
| Nicholas Smart | ST10433819 |
| Craig Harrod | ST10451648 |
| Kaellum Alves | ST10440806 |
| Pierre Louw | ST10437081 |

---

## 10. References

- Hypertext, 2024. *22seven is now Vault22, offering financial wellness to all.* https://htxt.co.za/2024/11/22seven-is-now-vault22-offering-financial-wellness-to-all/
- Goodbudget. *About us.* https://goodbudget.com/about-us/
- Schwahn, L., 2026. *What Is Cash Stuffing? How to Use the Envelope Budget System.* NerdWallet. https://www.nerdwallet.com/finance/learn/envelope-system
- Wait, R., 2025. *Wallet by BudgetBakers review.* Finder. https://www.finder.com/uk/budgeting/wallet-budgetbakers-review
- Budgetbakers, 2026. *What is the Wallet app?* https://support.budgetbakers.com/hc/en-us/articles/12212428113810-What-is-the-Wallet-app

---

<div align="center">
Made by the Budgie team
</div>
