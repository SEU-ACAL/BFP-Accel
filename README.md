ML Accelerator
=======================

## Getting Started

### Environment

mill version 0.11.1 (version above this may occured mistake).

verilator 5.024 is suitable.

```
sudo apt-get install libreadline-dev
```

### Quickly test

To run all tests in this design:
```bash
make run
```

<!-- To run individual test in this design:
```bash
make run ALL=<test_name>
``` -->

To generator system-verilog code 
```bash
make verilog
```

To run Vivado synthesis, generating timing report and utilization report
```bash
// take softmax as example, u need write your own script in scripts/
vivado -mode batch -source ./scripts/softmax.tcl 
```
