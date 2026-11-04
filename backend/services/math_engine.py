"""
F.R.I.D.A.Y. Deterministic Mathematical & Scientific Reasoning Engine
Powered by SymPy and Python Scientific computing.
Provides verified step-by-step calculations for Algebra, Calculus, Trigonometry, Statistics & Conversions.
"""

import math
import statistics
import re
from typing import Dict, Any, List, Optional
import sympy as sp
from sympy.parsing.sympy_parser import (
    parse_expr,
    standard_transformations,
    implicit_multiplication_application,
    convert_xor
)

TRANSFORMATIONS = standard_transformations + (
    implicit_multiplication_application,
    convert_xor
)


class MathEngine:
    """Deterministic mathematical computation engine."""

    def __init__(self):
        self.x, self.y, self.z, self.t = sp.symbols('x y z t')

    def _parse(self, expr_str: str) -> sp.Expr:
        """Safely parse mathematical expressions with implicit multiplication support."""
        cleaned = expr_str.strip()
        # Clean common words from natural language
        cleaned = re.sub(r'\b(derivative of|differentiate|integral of|integrate|solve|calculate|evaluate)\b', '', cleaned, flags=re.IGNORECASE).strip()
        # Handle powers like x^2
        return parse_expr(cleaned, transformations=TRANSFORMATIONS)

    def calculate_derivative(self, expr_str: str, variable: str = 'x', order: int = 1) -> Dict[str, Any]:
        """Calculates n-th order derivative with step-by-step derivation."""
        try:
            var = sp.Symbol(variable)
            expr = self._parse(expr_str)
            derivative = sp.diff(expr, var, order)

            steps = [
                f"Given function: f({variable}) = {expr}",
                f"Taking derivative with respect to '{variable}' (Order: {order})",
                f"Applying differentiation rules (Power, Product, Chain rules where applicable)",
                f"Result: {derivative}"
            ]

            return {
                "success": True,
                "operation": "derivative",
                "expression": str(expr),
                "variable": variable,
                "order": order,
                "result": str(derivative),
                "latex": sp.latex(derivative),
                "steps": steps
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def calculate_integral(self, expr_str: str, variable: str = 'x', lower: Optional[float] = None, upper: Optional[float] = None) -> Dict[str, Any]:
        """Calculates definite or indefinite integral with steps."""
        try:
            var = sp.Symbol(variable)
            expr = self._parse(expr_str)

            if lower is not None and upper is not None:
                # Definite integral
                integral = sp.integrate(expr, (var, lower, upper))
                steps = [
                    f"Given integrand: f({variable}) = {expr}",
                    f"Limits of integration: from {lower} to {upper}",
                    f"Antiderivative F({variable}) = {sp.integrate(expr, var)}",
                    f"Evaluating F({upper}) - F({lower})",
                    f"Definite Integral Result: {integral}"
                ]
            else:
                # Indefinite integral
                integral = sp.integrate(expr, var)
                steps = [
                    f"Given integrand: f({variable}) = {expr}",
                    f"Finding indefinite integral ∫ f({variable}) d{variable}",
                    f"Result: {integral} + C"
                ]

            return {
                "success": True,
                "operation": "integral",
                "expression": str(expr),
                "variable": variable,
                "definite": lower is not None and upper is not None,
                "result": str(integral) if (lower is not None and upper is not None) else f"{integral} + C",
                "latex": sp.latex(integral),
                "steps": steps
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def solve_equation(self, eq_str: str, variable: str = 'x') -> Dict[str, Any]:
        """Solves algebraic equations (e.g., quadratic, polynomial, linear)."""
        try:
            var = sp.Symbol(variable)
            if '=' in eq_str:
                lhs_str, rhs_str = eq_str.split('=', 1)
                lhs = self._parse(lhs_str)
                rhs = self._parse(rhs_str)
                eq = sp.Eq(lhs, rhs)
            else:
                eq = sp.Eq(self._parse(eq_str), 0)

            solutions = sp.solve(eq, var)
            steps = [
                f"Equation standard form: {eq.lhs - eq.rhs} = 0",
                f"Analyzing roots for variable: '{variable}'",
                f"Determined solutions: {', '.join(str(s) for s in solutions)}"
            ]

            return {
                "success": True,
                "operation": "solve_equation",
                "equation": str(eq),
                "variable": variable,
                "solutions": [str(s) for s in solutions],
                "latex": [sp.latex(s) for s in solutions],
                "steps": steps
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def calculate_statistics(self, numbers: List[float]) -> Dict[str, Any]:
        """Computes comprehensive statistical metrics."""
        try:
            if not numbers:
                return {"success": False, "error": "Empty dataset provided"}

            n = len(numbers)
            mean_val = statistics.mean(numbers)
            median_val = statistics.median(numbers)
            variance_val = statistics.variance(numbers) if n > 1 else 0.0
            stdev_val = statistics.stdev(numbers) if n > 1 else 0.0
            min_val = min(numbers)
            max_val = max(numbers)

            steps = [
                f"Dataset: {numbers} (Total elements: {n})",
                f"Sum: {sum(numbers)}",
                f"Mean (Average): sum / n = {sum(numbers)} / {n} = {mean_val:.4f}",
                f"Median (Middle Value): {median_val}",
                f"Sample Variance (s²): {variance_val:.4f}",
                f"Sample Standard Deviation (s = √s²): {stdev_val:.4f}"
            ]

            return {
                "success": True,
                "operation": "statistics",
                "count": n,
                "mean": round(mean_val, 4),
                "median": round(median_val, 4),
                "variance": round(variance_val, 4),
                "standard_deviation": round(stdev_val, 4),
                "min": min_val,
                "max": max_val,
                "steps": steps
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def convert_units(self, value: float, from_unit: str, to_unit: str) -> Dict[str, Any]:
        """Converts units of length, mass, temperature, speed, etc."""
        from_u = from_unit.strip().lower()
        to_u = to_unit.strip().lower()

        # Length conversions (base: meters)
        length_factors = {
            "m": 1.0, "meter": 1.0, "meters": 1.0, "metre": 1.0, "metres": 1.0,
            "km": 1000.0, "kilometer": 1000.0, "kilometre": 1000.0, "kilometres": 1000.0,
            "cm": 0.01, "centimeter": 0.01, "centimetre": 0.01,
            "mm": 0.001, "millimeter": 0.001,
            "mile": 1609.344, "miles": 1609.344,
            "yard": 0.9144, "yards": 0.9144,
            "foot": 0.3048, "feet": 0.3048, "ft": 0.3048,
            "inch": 0.0254, "inches": 0.0254, "in": 0.0254
        }

        # Mass conversions (base: grams)
        mass_factors = {
            "g": 1.0, "gram": 1.0, "grams": 1.0,
            "kg": 1000.0, "kilogram": 1000.0, "kilograms": 1000.0,
            "mg": 0.001, "milligram": 0.001,
            "lb": 453.59237, "pound": 453.59237, "pounds": 453.59237,
            "oz": 28.349523, "ounce": 28.349523, "ounces": 28.349523
        }

        if from_u in length_factors and to_u in length_factors:
            base = value * length_factors[from_u]
            result = base / length_factors[to_u]
            steps = [
                f"Converting length from {value} {from_unit} to {to_unit}",
                f"Value in base meters: {base} m",
                f"Converted value: {result:.6g} {to_unit}"
            ]
            return {"success": True, "result": result, "formatted": f"{result:.6g} {to_unit}", "steps": steps}

        if from_u in mass_factors and to_u in mass_factors:
            base = value * mass_factors[from_u]
            result = base / mass_factors[to_u]
            steps = [
                f"Converting mass from {value} {from_unit} to {to_unit}",
                f"Value in base grams: {base} g",
                f"Converted value: {result:.6g} {to_unit}"
            ]
            return {"success": True, "result": result, "formatted": f"{result:.6g} {to_unit}", "steps": steps}

        # Temperature
        if ("celsius" in from_u or from_u == "c") and ("fahrenheit" in to_u or to_u == "f"):
            res = (value * 9 / 5) + 32
            return {"success": True, "result": res, "formatted": f"{res:.2f} °F", "steps": [f"({value}°C × 9/5) + 32 = {res:.2f}°F"]}
        if ("fahrenheit" in from_u or from_u == "f") and ("celsius" in to_u or to_u == "c"):
            res = (value - 32) * 5 / 9
            return {"success": True, "result": res, "formatted": f"{res:.2f} °C", "steps": [f"({value}°F - 32) × 5/9 = {res:.2f}°C"]}

        return {"success": False, "error": f"Unsupported conversion from '{from_unit}' to '{to_unit}'"}

    def evaluate_general(self, expr_str: str) -> Dict[str, Any]:
        """Evaluates arbitrary algebraic or trigonometric expressions."""
        try:
            expr = self._parse(expr_str)
            simplified = sp.simplify(expr)
            numeric_val = simplified.evalf() if hasattr(simplified, 'evalf') else None

            steps = [
                f"Original expression: {expr_str}",
                f"Parsed SymPy AST: {expr}",
                f"Simplified expression: {simplified}"
            ]
            if numeric_val is not None:
                steps.append(f"Decimal approximation: {numeric_val}")

            return {
                "success": True,
                "operation": "evaluate",
                "simplified": str(simplified),
                "numeric": str(numeric_val) if numeric_val else None,
                "latex": sp.latex(simplified),
                "steps": steps
            }
        except Exception as e:
            return {"success": False, "error": str(e)}


math_engine = MathEngine()

