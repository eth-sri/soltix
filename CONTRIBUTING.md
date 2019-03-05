# How to contribute

We welcome contributions of any kind - such as new program features, bug reports,
bugfixes, and ideas for further improvements. If you are interested in making
significant changes, please do not hesitate to contact us in advance to discuss 
details.

Some ideas for possible improvements are listed below.


## Portability and configuration

The framework is currently limited to using truffle and Ganache CLI as execution
backend, and could benefit from added support for other blockchain
implementations such as aleth. This change could be approached by adapting
truffle to work without Ganache CLI, thereby providing benefits to the broader
Ethereum ecosystem as well as minimizing the amount of changes needed within ECTF.

Support for docker could allow for an easier and more portable installation. The
execution is driven by Unix shell scripts, which could also be replaced with
e.g. Python scripts to support Windows and other operating systems natively.

Various properties of predicates and other expression generation functions could
be made configurable. Some of these already reference hardcoded values from the
Configuration class (such as randomPredicateDepth()), and could be stored in a 
configuration file or passed via command line options instead. This also applies
to various code generator classes that choose expressions and types with
hardcoded probabilities.



## Code generation extensions

The framework currently only supports the three live code mutations described by
["Finding  Compiler  Bugs  via  Live  Code  Mutation" (Sun et al., 2014)](http://web.cs.ucdavis.edu/~su/publications/oopsla16.pdf).
New mutation types could be devised and implemented, potentially taking into 
account knowledge on the internals of the solc compiler to exercise specific
optimizations.

The code generation heavily favors basic computational constructs such as arithmetic
operations and control structures, and could be extended with additional language
constructs like modifiers and more blockchain-specific functionality (value transfer,
address-related computations in general, and cryptographic functions).


