import os
import pytest
import requests

GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://localhost:80")
AUTH_BASE = f"{GATEWAY_URL}/api/v1/auth"
HR_BASE = f"{GATEWAY_URL}/api/v1/hr"


def pytest_configure(config):
    config.addinivalue_line(
        "markers", "integration: tests requiring the full stack (gateway + auth + hr)"
    )


@pytest.fixture(scope="session")
def gateway_url():
    return GATEWAY_URL


@pytest.fixture(scope="session")
def auth_base():
    return AUTH_BASE


@pytest.fixture(scope="session")
def hr_base():
    return HR_BASE


@pytest.fixture(scope="session")
def rh_token(auth_base):
    """JWT d'un RH Manager — obtenu via login sur la passerelle."""
    resp = requests.post(
        f"{auth_base}/login/",
        json={"email": os.environ["TEST_RH_EMAIL"], "password": os.environ["TEST_RH_PASSWORD"]},
        timeout=10,
    )
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    return resp.json()["access"]


@pytest.fixture(scope="session")
def rh_headers(rh_token):
    return {"Authorization": f"Bearer {rh_token}"}
