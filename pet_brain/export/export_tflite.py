"""TFLite export placeholder.

The supported first deployment path is PyTorch -> ONNX -> ONNX Runtime Mobile.
TFLite conversion is intentionally not attempted until an ONNX graph is
verified numerically against PyTorch and a converter is selected explicitly.
"""


def main() -> None:
    raise SystemExit("TFLite export is not enabled yet; export and validate ONNX first.")


if __name__ == "__main__":
    main()
