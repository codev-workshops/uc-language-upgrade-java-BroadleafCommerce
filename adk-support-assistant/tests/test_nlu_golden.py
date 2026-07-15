"""NLU golden-dataset accuracy tests (per language) with a threshold."""

import json

import pytest

from adk_support_assistant.nlu.classifier import RuleBasedNluClient

from .conftest import DATA_DIR


def _load(name):
    return json.loads((DATA_DIR / name).read_text(encoding="utf-8"))


@pytest.mark.parametrize("dataset", ["nlu_golden_en.json", "nlu_golden_es.json"])
def test_nlu_accuracy_threshold(flow, dataset):
    data = _load(dataset)
    locale = data["locale"]
    threshold = data["min_accuracy"]
    nlu = RuleBasedNluClient(flow)

    correct = 0
    entity_checks = 0
    entity_correct = 0
    failures = []
    for case in data["cases"]:
        result = nlu.classify(case["text"], locale)
        if result.intent == case["intent"]:
            correct += 1
        else:
            failures.append((case["text"], case["intent"], result.intent))
        for key, expected in case.get("entities", {}).items():
            entity_checks += 1
            if result.entities.get(key) == expected:
                entity_correct += 1

    accuracy = correct / len(data["cases"])
    assert accuracy >= threshold, f"{dataset} accuracy {accuracy:.2f} < {threshold}; misses={failures}"
    if entity_checks:
        entity_acc = entity_correct / entity_checks
        assert entity_acc >= 0.9, f"{dataset} entity accuracy {entity_acc:.2f}"
