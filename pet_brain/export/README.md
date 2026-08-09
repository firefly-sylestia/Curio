# Export boundary

The first supported deployment route is:

1. Train and checkpoint in PyTorch.
2. Export with `python -m pet_brain.export.export_onnx checkpoint.pt model.onnx`.
3. Compare PyTorch and ONNX outputs on a fixed observation/hidden-state test
   set before shipping.
4. Add the verified graph to Android assets and run it with ONNX Runtime Mobile.
5. Persist `hidden_out` between interaction calls separately from model weights.
6. Keep the Android neural setting off until a longer-trained checkpoint passes
   numerical parity and behavioral evaluation; the 10-episode smoke checkpoint
   is only a pipeline test.

The wrapper exposes `observations` and `hidden_in`, and returns action logits,
value, emotion, needs, memory, and `hidden_out`. Dynamic batch and sequence
axes are declared. TFLite is not enabled because conversion without numerical
verification would risk changing recurrent behavior silently.
