/*
 * SOLTIX - Scalable automated framework for testing Solidity compilers.
 *
 * Author: Nils Weller <nweller@uni-bremen.de>
 *
 * Copyright (C) 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package soltix.interpretation;

import soltix.ast.*;
import soltix.interpretation.*;
import soltix.profiling.*;

import java.util.ArrayList;

/**
 * Class to (heuristically) calculate stack usage in order to avoid running into limits.
 */
public class StackCalculator {
    static protected int computeModifierStackSize(ASTFunctionDefinition function) {
        ArrayList<ASTModifierInvocation> modifiers = function.getModifiers();
        int result = 0;
        if (modifiers != null) {
            for (ASTModifierInvocation modifier : modifiers) {
                ASTModifierDefinition definition = modifier.getDefinition();
                if (definition != null) { // see ASTInterpreter: TODO: This is probably not really a modifier invocation...
                    result += definition.getLocalDeclarationsCount() + definition.getParameterCount();
                }
            }
        }
        return result;
    }

    static protected int computeStackSize(FunctionScope function) {
        int result;
        result =  function.getParameterList().getChildCount()
                + function.getLocalDeclarationsCount();

        if (function instanceof ASTFunctionDefinition) {
            ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition)function;
            result +=  computeModifierStackSize(functionDefinition)
                     + functionDefinition.getReturnValuesCount();
        }
        return result;
    }

    static public boolean isProfilingEventFull(FunctionScope function, ProfilingEvent event) throws Exception {
        // TODO: Our stack size model still isn't quite correct. Using a limit of 10 to provide an extra buffer works
        // for the Securify suite
        final int maximumStackSize = 12; //15;
        int stackSizeWithoutEvent = computeStackSize(function);

        /*
        int eventStackSize = event.getArguments() == null? 0: event.getArguments().size();
        // There's apparently something special about passing variables as string event arguments that causes
        // them to take up twice the amount of stack space - account for this:
        eventStackSize += event.getStringArgumentCount();

        int totalStackSize = stackSizeWithoutEvent + eventStackSize;
        event.setLastComputedStackSize(totalStackSize); // for debugging
*/
        /* This isn't working either, revert to event accounting:


        // The stack situation is very unclear. The event argument count per se does not seem to add to
        // the function's stack. The event count is limited to 17, but we pick a lower limit:
        int eventStackSize = event.getArguments() == null? 0: event.getArguments().size();
        if (eventStackSize >= 10) {
            return true;
        }
        // ... now what's true is that event arguments can affect the stack size if they reference
        // function parameters or string variables. This could be a question of parameters being
        // optimized away unless they are referenced - but we always account for all parameters, so
        // this would not need to be treated separately.
        // We include string arguments in the total size as well.
        int totalStackSize = stackSizeWithoutEvent + event.getStringArgumentCount();
*/
        int eventStackSize = event.getArguments() == null? 0: event.getArguments().size();
        // There's apparently something special about passing variables as string event arguments that causes
        // them to take up twice the amount of stack space - account for this:
        eventStackSize += event.getStringArgumentCount();

        int totalStackSize = stackSizeWithoutEvent + eventStackSize;
        event.setLastComputedStackSize(totalStackSize); // for debugging

        /*if (totalStackSize > maximumStackSize && eventStackSize > 0) {
            throw new Exception("isProfilingEventFull received unexpectedly bloated event");
        } else*/ if (totalStackSize >= maximumStackSize) {
            return true; // is full
        } else {
            return false; // there's still room
        }
    }
}
