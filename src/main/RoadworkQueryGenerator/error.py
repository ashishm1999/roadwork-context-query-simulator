"""Typed exceptions used by the roadwork query generator."""

from __future__ import annotations


class GeneratorError(Exception):
    """Base class for every error the generator can raise."""


class ConfigurationError(GeneratorError):
    """Raised when config.ini is missing a required section or key."""


class DatasetError(GeneratorError):
    """Raised when a dataset file is missing, empty, or malformed."""


class DistributionError(GeneratorError):
    """Raised when a probability distribution cannot be constructed."""


class TemplateError(GeneratorError):
    """Raised when a query template can not be materialised."""
