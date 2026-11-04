"""
F.R.I.D.A.Y. Deterministic Mathematics Router
Exposes dedicated endpoints for derivatives, integrals, equations, statistics, and conversions.
"""

from typing import Optional, List, Dict, Any
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from services.math_engine import math_engine

router = APIRouter(prefix="/api/v1/math", tags=["Mathematics"])


class DerivativeRequest(BaseModel):
    expression: str = Field(..., description="Mathematical expression (e.g. 'x^2 + 5*x')")
    variable: str = Field(default="x")
    order: int = Field(default=1, ge=1, le=5)


class IntegralRequest(BaseModel):
    expression: str = Field(..., description="Integrand expression")
    variable: str = Field(default="x")
    lower_bound: Optional[float] = None
    upper_bound: Optional[float] = None


class EquationRequest(BaseModel):
    equation: str = Field(..., description="Equation (e.g. 'x^2 - 4 = 0')")
    variable: str = Field(default="x")


class StatisticsRequest(BaseModel):
    numbers: List[float] = Field(..., min_length=1)


class ConversionRequest(BaseModel):
    value: float
    from_unit: str
    to_unit: str


@router.post("/derivative")
def compute_derivative(req: DerivativeRequest):
    res = math_engine.calculate_derivative(req.expression, variable=req.variable, order=req.order)
    if not res["success"]:
        raise HTTPException(status_code=400, detail=res.get("error"))
    return res


@router.post("/integral")
def compute_integral(req: IntegralRequest):
    res = math_engine.calculate_integral(req.expression, variable=req.variable, lower=req.lower_bound, upper=req.upper_bound)
    if not res["success"]:
        raise HTTPException(status_code=400, detail=res.get("error"))
    return res


@router.post("/solve")
def solve_equation(req: EquationRequest):
    res = math_engine.solve_equation(req.equation, variable=req.variable)
    if not res["success"]:
        raise HTTPException(status_code=400, detail=res.get("error"))
    return res


@router.post("/statistics")
def compute_statistics(req: StatisticsRequest):
    res = math_engine.calculate_statistics(req.numbers)
    if not res["success"]:
        raise HTTPException(status_code=400, detail=res.get("error"))
    return res


@router.post("/convert")
def convert_units(req: ConversionRequest):
    res = math_engine.convert_units(req.value, req.from_unit, req.to_unit)
    if not res["success"]:
        raise HTTPException(status_code=400, detail=res.get("error"))
    return res

