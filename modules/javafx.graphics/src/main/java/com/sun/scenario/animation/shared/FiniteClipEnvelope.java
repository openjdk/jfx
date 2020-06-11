/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.scenario.animation.shared;

import com.sun.javafx.util.Utils;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.util.Duration;

/**
 * Clip envelope implementation for multi-cycles: cycleCount != (1 or indefinite) and cycleDuration != indefinite
 */
public class FiniteClipEnvelope extends MultiLoopClipEnvelope {

    private int cycleCount;
    private long totalTicks;

    protected FiniteClipEnvelope(Animation animation) {
        super(animation);
        if (!animation.getCuePoints().isEmpty())
            System.out.println("FiniteClipEnvelope");
        if (animation != null) {
            autoReverse = animation.isAutoReverse();
            cycleCount = animation.getCycleCount();
        }
        updateTotalTicks();
    }

    @Override
    public ClipEnvelope setCycleDuration(Duration cycleDuration) {
        if (cycleDuration.isIndefinite()) {
            return create(animation);
        }
        updateCycleTicks(cycleDuration);
        updateTotalTicks();
        return this;
    }

    @Override
    public ClipEnvelope setCycleCount(int cycleCount) {
        if ((cycleCount == 1) || (cycleCount == Animation.INDEFINITE)) {
            return create(animation);
        }
        this.cycleCount = cycleCount;
        updateTotalTicks();
        return this;
    }

    private void updateTotalTicks() {
        totalTicks = cycleCount * cycleTicks;
    }

    @Override
    public void setRate(double newRate) {
//        final boolean toggled = isDirectionChanged(newRate);
//        final long newTicks = toggled ? totalTicks - ticks : ticks;
        if (animation.getStatus() != Status.STOPPED) {
//          deltaTicks = newTicks - (switchedDirection ? -1 : 1) * ticksRateChange(newRate); d + T - 2t
            deltaTicks = ticks - ticksRateChange(newRate);
            abortCurrentPulse();
        }
//        ticks = newTicks;
        System.out.println("Finite setRate ticks = " + ticks);
        rate = newRate;
    }

    // Fails on auto-reverse even cycle count when switching
   @Override
   protected boolean isDuringEvenCycle() {
       System.out.println("startPositive = " + startedPositive);
       System.out.println("rate > 0 = " + (rate > 0));
       System.out.println("cycleCount % 2 = " + (cycleCount % 2 == 0));
       if (rate > 0) {
           if (!startedPositive && cycleCount % 2 == 0) {
               return isDuringNegEvenCycle();
           }
           return isDuringPosEvenCycle();
//           System.out.println("isDuringEvenCycle rate > 0 = " + b);
//           return b;
       } else {
           if (startedPositive && cycleCount % 2 == 0) {
               return isDuringPosEvenCycle();
           }
           return isDuringNegEvenCycle();
           
//           System.out.println("isDuringEvenCycle rate < 0 = " + b);
//           return b;
       }
   }

   protected boolean isDuringPosEvenCycle() {
       return ticks % (2 * cycleTicks) < cycleTicks;
   }

   protected boolean isDuringNegEvenCycle() {
       return (totalTicks - ticks) % (2 * cycleTicks) < cycleTicks;
   }

   @Override
   public void timePulse(long destinationTick) {
       if (cycleTicks == 0L) {
           return;
       }
       aborted = false;
       inTimePulse = true;

       System.out.println("dest = " + destinationTick);

       try {
           double currentRate = calculateCurrentRunningRate();
           System.out.println("rate, curRate = " + rate + ", " + currentRate);
           final long oldTicks = ticks;
           System.out.println("oldTicks = " + oldTicks);
           System.out.println("deltaTicks = " + deltaTicks);
           destinationTick = Math.round(destinationTick * currentRate);
           if (autoReverse && !isDuringEvenCycle()) {
               destinationTick = -destinationTick;
           }
           System.out.println("new dest = " + destinationTick);
//           long ticksChange = Math.round(destinationTick * Math.abs(rate));
           ticks = Utils.clamp(0, deltaTicks + destinationTick, totalTicks);
           System.out.println("ticks = " + ticks);

           // overall delta between current position and new position. always >= 0
           long overallDelta = Math.abs(ticks - oldTicks);
           System.out.println("overallDelta = " + overallDelta);
           if (overallDelta == 0) {
               System.out.println("delta = 0");
//               return;
           }

           System.out.println("cyclePos = " + cyclePos);
//           currentRate = calculateCurrentRunningRate();
//           System.out.println("Finite pulse rate = " + currentRate);

           final boolean reachedEnd = (rate > 0) ? (ticks == totalTicks) : (ticks == 0);
           System.out.println("reachedEnd = " + reachedEnd);

           // delta to reach end of cycle, always >= 0. 0 if at the start/end of a cycle
           long cycleDelta = currentRate > 0 ? cycleTicks - cyclePos : cyclePos;
           System.out.println("cycleDelta = " + cycleDelta);

           // check if the end of the cycle is inside the range of [currentTick, destinationTick]
           // If yes, advance to the end of the cycle and pass the rest of the ticks to the next cycle.
           // If the next cycle is completed, continue to the next etc.
//           long leftoverTicks = Math.abs(overallDelta) - cycleDelta;
           while (overallDelta >= cycleDelta) {
               cyclePos = (currentRate > 0) ? cycleTicks : 0;
               System.out.println("finishing cycle cyclePos = " + cyclePos + " ------------------------");
               AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
               if (aborted) {
                   return;
               }
               overallDelta -= cycleDelta;
               System.out.println("leftover delta = " + overallDelta);

               if (overallDelta > 0 || !reachedEnd) {
                   if (autoReverse) { // change direction
                       setCurrentRate(-currentRate);
                       currentRate = -currentRate;
                       System.out.println("switching direction to " + currentRate + " ------------------------");
                   } else { // jump back to the the cycle
                       cyclePos = (currentRate > 0) ? 0 : cycleTicks;
                       System.out.println("restaring cycle cyclePos = " + cyclePos + " ------------------------");
                       AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
                   }
               }
               cycleDelta = cycleTicks;
           }

           if (overallDelta > 0/* && !reachedEnd */) {
//               cyclePos += Math.signum(currentRate) * overallDelta;
               cyclePos += (currentRate > 0) ? overallDelta : -overallDelta;
               System.out.println("new cyclePos = " + cyclePos);
               AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
           }

           if (reachedEnd && !aborted) {
               System.out.println("finished");
               AnimationAccessor.getDefault().finished(animation);
           }
           System.out.println();

       } finally {
           inTimePulse = false;
       }
   }

    @Override
    public void jumpTo(long newTicks) {
        if (cycleTicks == 0L) {
            return;
        }

        final long oldTicks = ticks;
//        if (rate < 0) {
//            newTicks = totalTicks - newTicks;
//        }
        ticks = Utils.clamp(0, newTicks, totalTicks);
        final long delta = ticks - oldTicks;
//        System.out.println("delta = " + delta);
        if (delta == 0) {
//            System.out.println();
//            return;
        }
        deltaTicks += delta;
        cyclePos = ticks % cycleTicks;
        if (autoReverse) {
//            pos = ticks % cycleTicks;
            if (animation.getStatus() == Status.RUNNING) {
                setCurrentRate(calculateCurrentRunningRate()); // needed? a pulse calculates the rate anyway
            }
//            if (isDuringForwardCycle() == (rate > 0)) { // TODO: should use calculateCurrentRate
////                pos = ticks % cycleTicks;
//                if (animation.getStatus() == Status.RUNNING) {
//                    // this rate is never 0, even though the anim one is, so setting the current
//                    // rate can be wrong because it is based on an older rate
//                    setCurrentRate(Math.abs(rate));
//                }
//            } else {
// //               pos = cycleTicks - (ticks % cycleTicks);
//                if (animation.getStatus() == Status.RUNNING) {
//                    setCurrentRate(-Math.abs(rate));
//                }
//            }
        } //else {
//            pos = ticks % cycleTicks;
//            if (rate < 0) {
//                pos = cycleTicks - pos;
//            }
            if ((cyclePos == 0) && (ticks != 0)) { // TODO: check when pos = 0 and when pos = cycleticks
                cyclePos = cycleTicks;
            }
 //       }

//        System.out.println("pos = " + TickCalculation.toMillis(pos));
//        System.out.println("rate = " + rate);
        AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
        abortCurrentPulse();

//        System.out.println();
    }
}
