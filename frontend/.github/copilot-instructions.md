# Copilot Instructions - Real-Time Trading App Frontend

## Project Overview
A React + Vite crypto trading application with Redux state management, using Radix UI components and Tailwind CSS.

## Architecture & Key Patterns

### Redux State Structure
- **Location**: `src/State/`
- **Structure**: Modular by domain (Auth, Coin)
- **Pattern**: Redux Thunk for async actions
  ```
  Store.js (legacy_createStore + applyMiddleware(thunk))
  ├── Auth/
  │   ├── Action.js (async dispatch functions: register, login, getUser, logout)
  │   ├── ActionTypes.js (REGISTER_*, LOGIN_*, GET_USER_*, LOGOUT)
  │   └── Reducer.js (manages user, jwt, loading, error)
  └── Coin/
      ├── Action.js (fetch coin data from external APIs)
      └── Reducer.js (manages coin list, market data)
  ```
- **Key State Properties**:
  - `auth.user`: Current authenticated user object
  - `auth.jwt`: Bearer token for API requests (localStorage backup)
  - `auth.loading`: Request in progress
  - `coin`: Cryptocurrency market data

### Component Organization
- **Pages**: `src/pages/` - Route-level components (Auth, Home, Wallet, Portfolio, etc.)
- **UI Components**: `src/components/ui/` - Shadcn/Radix UI primitives (Button, Card, Dialog, Form, Input, etc.)
- **Assets**: `src/assets/` - Images and static files
- **Config**: `src/config/api.js` - API endpoints and axios configuration

### Critical Authentication Flow
1. User submits credentials → `SigninForm.jsx` dispatches `login(userData)`
2. `Action.js:login()` → POST to `/auth/signin` → JWT saved to localStorage + Redux
3. `getUser(jwt)` fetches full user profile and stores in `auth.user`
4. `App.jsx` conditionally renders: `auth.user ? <AppRoutes/> : <Auth/>`
5. All API calls use `Authorization: Bearer ${jwt}` header (see `Action.js`)

## Common Workflows

### Adding a New Form (Pattern Used Throughout)
1. Import `useForm` from `react-hook-form` with default values
2. Use `Form`, `FormField`, `FormItem`, `FormControl`, `FormMessage` from UI
3. Dispatch Redux action in `onSubmit`
4. Example: `src/pages/Auth/SignupForm.jsx`, `src/pages/Wallet/TopupForm.jsx`

### API Integration (Redux Thunk Pattern)
```javascript
// In Action.js
export const actionName = (params) => async (dispatch) => {
  dispatch({ type: REQUEST });
  try {
    const response = await axios.post(`${BASE_URL}/endpoint`, params);
    dispatch({ type: SUCCESS, payload: response.data });
  } catch (error) {
    dispatch({ type: FAILURE, payload: error.message });
  }
};
```

### Styling Convention
- **Framework**: Tailwind CSS (configured in `tailwind.config.js`)
- **Component Library**: Shadcn/Radix UI (pre-built buttons, dialogs, forms)
- **Pattern**: Utility classes + component composition
  - Example: `className='border w-full border-gray-700 p-5'`
  - Responsive classes: `text-sm lg:text-base`, `w-full lg:w-[60%]`

## Development Commands
```bash
npm run dev      # Start Vite dev server (http://localhost:5173)
npm run build    # Production build
npm run lint     # ESLint check
npm run preview  # Preview production build
```

## Key Dependencies & Usage
- **React 19.1.1**: Core framework
- **Redux + Redux Thunk**: State management (legacy setup, NOT Redux Toolkit)
- **React Hook Form**: Form state management (with defaultValues, no validation resolver initially)
- **Axios**: HTTP client (interceptors available in `src/config/api.js`)
- **Shadcn/Radix UI**: Component primitives (Avatar, Dialog, Button, Card, etc.)
- **Tailwind CSS**: Utility-first styling
- **ApexCharts**: Chart visualization for market data
- **Lucide React**: Icons (imports like `Wallet`, `Copy`, `RefreshCw`, not `*Icon`)

## Common Pitfalls & Fixes

### 1. Controlled/Uncontrolled Input Warning
- **Issue**: Form inputs initialized with `undefined` instead of empty string
- **Fix**: Always set `defaultValues: { fieldName: "" }` in `useForm()`

### 2. Icon Import Errors
- **Issue**: Lucide imports fail (e.g., `WalletIcon` doesn't exist)
- **Fix**: Use correct names without `Icon` suffix: `Wallet`, `Copy`, `RefreshCw`
- **Radix Icons**: Use `@radix-ui/react-icons` (e.g., `BookmarkFilledIcon`, `DotIcon`)

### 3. Redux Store Not Initializing
- **Issue**: Provider receives undefined store
- **Fix**: Ensure `Store.js` exports default, and `main.jsx` imports it: `import store from './State/Store'`
- **Setup**: Using legacy Redux (not Toolkit), so `legacy_createStore + applyMiddleware(thunk)`

### 4. Navigation After Auth
- **Pattern**: Dispatch login action → `getUser()` → navigate to home
- **Don't**: Pass navigate to dispatch directly; call navigate in callback after dispatch resolves

### 5. JWT Token Management
- **Storage**: Both localStorage AND Redux state (redundancy for page refresh)
- **API Headers**: Include `Authorization: Bearer ${jwt}` in all protected endpoints
- **Logout**: Clear localStorage + dispatch LOGOUT action

## File Naming Conventions
- **Components**: PascalCase (`Navbar.jsx`, `SignupForm.jsx`)
- **Directories**: PascalCase (`src/pages/Auth/`, `src/State/Auth/`)
- **Actions/Reducers**: camelCase (`Action.js`, `Reducer.js`, `ActionTypes.js`)
- **Note**: Some directories have spaces (`Payment Details/`, `Stock Detials/`) - maintain as-is

## Testing & Debugging
- **Redux DevTools**: Recommended for state inspection (not currently installed)
- **Console Logs**: Used throughout for debugging (e.g., `console.log("auth -- ", auth)`)
- **Network Tab**: Verify API calls to `http://localhost:8080`
- **localStorage**: Inspect JWT token persistence

## Key Integration Points
- **Backend**: Assumed to run on `http://localhost:8080` (see `Action.js` baseURL)
- **Endpoints**:
  - `POST /auth/signup` - Register new user
  - `POST /auth/signin` - Login user
  - `GET /api/users/profile` - Fetch user profile (requires Bearer token)
- **CoinGecko API**: Used for market data (referenced in chart components)

---
**Last Updated**: November 2025 | For questions, refer to `src/State/` structure and `src/pages/` routing patterns.
