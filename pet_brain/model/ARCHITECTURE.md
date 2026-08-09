# Pet Brain Emotional v3 compatibility

The supplied archive is intentionally preserved outside the Python package and
is not loaded as a checkpoint.

`model.json` declares 8,225,640 parameters and the raw file contains exactly
8,225,640 little-endian float32 values. The supplied `pet_brain.py` uses
`torch.nn.GRU(128, 1536, batch_first=True)` plus four linear heads. That source
architecture has 8,230,248 parameters when instantiated in PyTorch: the
4,608-parameter difference is one complete GRU bias vector. There is no
reliable ordering/shape manifest for the flat binary, so reshaping it would be
an unsafe silent conversion.

This project therefore recreates the documented backbone with fresh Xavier-like
initialization and adds trainable PPO heads:

- actor: 64 action logits
- critic: scalar value
- emotion: 24 tanh channels
- needs: 16 sigmoid channels
- memory: 256 projection channels
- action-conditioned next-state prediction: 128 channels

The raw binary remains an external source artifact. Future conversion is only
safe after a manifest proves parameter ordering, tensor shapes, bias policy,
and checksum compatibility.
