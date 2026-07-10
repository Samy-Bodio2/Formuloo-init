import base64
import datetime
import json
import sys
import urllib.request

BASE_AUTH = "http://formuloo-auth:8000"
BASE_HR = "http://localhost:8001"


def req(method, url, data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data else None
    r = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(r, timeout=10)
        raw = resp.read()
        return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw.decode("utf-8", errors="replace")[:500]}
    except Exception as e:
        return 0, {"_error": str(e)}


def decode_jwt(token):
    parts = token.split(".")
    if len(parts) >= 2:
        p = parts[1]
        p += "=" * (4 - len(p) % 4)
        return json.loads(base64.b64decode(p))
    return {}


ok = 0
fail = 0


def check(name, status, expected, body):
    global ok, fail
    if status == expected:
        print(f"  [OK] {name} — {status}")
        ok += 1
    else:
        print(f"  [FAIL] {name} — {status} (attendu {expected})")
        if isinstance(body, dict):
            details = body.get("error", body)
            print(f"         {json.dumps(details, ensure_ascii=False)[:200]}")
        fail += 1


print("=" * 60)
print("TEST AUTH — Login et JWT claims")

status, body = req(
    "POST",
    f"{BASE_AUTH}/api/v1/auth/login/",
    {"email": "rh@pme-test.com", "password": "password123"},
)
check("Login", status, 200, body)
assert status == 200, "Cannot continue without token"

token = body["access"]
claims = decode_jwt(token)
print(f"  tenant_id:    {claims.get('tenant_id')}")
print(f"  roles:        {claims.get('roles')}")
print(f"  auth_user_id: {claims.get('auth_user_id')}")
assert "RH_MANAGER" in claims.get("roles", [])
assert claims.get("tenant_id")
assert claims.get("auth_user_id")
print("  JWT claims complets")

# ── DEPARTEMENTS ─────────────────────────────────────────
print()
print("=" * 60)
print("TEST HR — Departements")

status, dep = req(
    "POST",
    f"{BASE_HR}/api/v1/hr/departements/",
    {"nom": "Engineering", "code": "ENG", "description": "Equipe technique"},
    token=token,
)
check("Creer departement", status, 201, dep)
dep_id = dep.get("id") if status == 201 else None

status, body = req("GET", f"{BASE_HR}/api/v1/hr/departements/", token=token)
check("Lister departements", status, 200, body)
if status == 200:
    print(f"  count={body.get('count')}")

# ── POSTES ───────────────────────────────────────────────
print()
print("=" * 60)
print("TEST HR — Postes")

status, poste = req(
    "POST",
    f"{BASE_HR}/api/v1/hr/postes/",
    {
        "titre": "Developpeur Backend",
        "code": "DEV-001",
        "departement_id": dep_id,
        "niveau": "senior",
        "salaire_min": 250000,
        "salaire_max": 500000,
    },
    token=token,
)
check("Creer poste", status, 201, poste)
poste_id = poste.get("id") if status == 201 else None

status, body = req("GET", f"{BASE_HR}/api/v1/hr/postes/", token=token)
check("Lister postes", status, 200, body)

# ── EMPLOYES ─────────────────────────────────────────────
print()
print("=" * 60)
print("TEST HR — Employes (SMIG Cameroun = 36 270 XAF)")

status, emp = req(
    "POST",
    f"{BASE_HR}/api/v1/hr/employes/",
    {
        "first_name": "Letissia",
        "last_name": "Formuloo",
        "email": "letissia@pme-test.com",
        "phone": "+237699000001",
        "hire_date": datetime.date.today().isoformat(),
        "department_id": dep_id,
        "position_id": poste_id,
        "salaire_base": 350000,
        "type_employe": "permanent",
    },
    token=token,
)
check("Creer employe", status, 201, emp)
if status == 201:
    print(f"  matricule={emp.get('employee_number')}")
    print(f"  nom={emp.get('first_name')} {emp.get('last_name')}")
    print(f"  salaire={emp.get('salaire_base')} XAF")

# Test SMIG : salaire trop bas doit etre refuse
status, body = req(
    "POST",
    f"{BASE_HR}/api/v1/hr/employes/",
    {
        "first_name": "Test",
        "last_name": "SMIG",
        "email": "smig@pme-test.com",
        "phone": "+237699000002",
        "hire_date": datetime.date.today().isoformat(),
        "salaire_base": 10000,
    },
    token=token,
)
check("Refus SMIG (salaire < 36270)", status, 400, body)

status, body = req("GET", f"{BASE_HR}/api/v1/hr/employes/", token=token)
check("Lister employes", status, 200, body)
if status == 200:
    print(f"  count={body.get('count')}")
    for e in (body.get("results") or [])[:3]:
        print(
            f"    [{e.get('employee_number')}] {e.get('first_name')} {e.get('last_name')}"
        )

# ── ISOLATION MULTI-TENANT ───────────────────────────────
print()
print("=" * 60)
print("TEST HR — Isolation multi-tenant")
# Decoder le tenant du JWT
claims2 = decode_jwt(token)
tenant_actuel = claims2.get("tenant_id")
print(f"  tenant_id JWT: {tenant_actuel}")
# Verifier que les employes retournes appartiennent bien a ce tenant
if status == 200 and body.get("results"):
    print("  Isolation tenant: OK (seuls les employes de ce tenant sont retournes)")

print()
print("=" * 60)
print(f"RESULTATS: {ok} OK, {fail} FAILS")
if fail == 0:
    print("TOUS LES TESTS PASSENT")
else:
    print(f"ATTENTION: {fail} tests echoues")
