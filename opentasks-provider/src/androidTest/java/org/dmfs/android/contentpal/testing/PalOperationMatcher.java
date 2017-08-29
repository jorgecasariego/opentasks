/*
 * Copyright 2017 dmfs GmbH
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

package org.dmfs.android.contentpal.testing;

import android.content.ContentProviderClient;
import android.content.Context;

import org.dmfs.android.contentpal.OperationsBatch;
import org.dmfs.android.contentpal.OperationsQueue;
import org.dmfs.android.contentpal.queues.BasicOperationsQueue;
import org.dmfs.optional.Absent;
import org.dmfs.optional.Optional;
import org.dmfs.optional.Present;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;


/**
 * @author Gabor Keszthelyi
 */
public final class PalOperationMatcher extends TypeSafeDiagnosingMatcher<OperationsBatch>
{
    private final Context mContext;
    private final String mAuthority;
    private final OperationCheck[] mChecks;

    private Optional<String> mFailMsg = new Absent<>();


    public PalOperationMatcher(Context context, String authority, OperationCheck... checks)
    {
        mContext = context;
        mAuthority = authority;
        mChecks = checks;
    }


    public static Matcher<OperationsBatch> resultsIn(Context context, String authority, OperationCheck... checks)
    {
        return new PalOperationMatcher(context, authority, checks);
    }


    @Override
    protected boolean matchesSafely(OperationsBatch batch, Description mismatchDescription)
    {
        // TODO Revisit this (TypeSafeDiagnosingMatcher calls this method 2 times, first with no-op mismatchDescription)
        if (mFailMsg.isPresent())
        {
            mismatchDescription.appendText(mFailMsg.value());
            return false;
        }

        ContentProviderClient client = mContext.getContentResolver().acquireContentProviderClient(mAuthority);

        for (OperationCheck operationCheck : mChecks)
        {
            StringBuilder failMessage = new StringBuilder();
            if (!operationCheck.beforeCheck().isSatisfied(client, failMessage))
            {
                mFailMsg = new Present<>("Before check failed with: " + failMessage);
                return false;
            }
        }

        OperationsQueue operationsQueue = new BasicOperationsQueue(client);
        try
        {
            operationsQueue.enqueue(batch);
            operationsQueue.flush();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception during executing the OperationBatch", e);
        }

        for (OperationCheck operationCheck : mChecks)
        {
            StringBuilder failMessage = new StringBuilder();
            if (!operationCheck.afterCheck().isSatisfied(client, failMessage))
            {
                mFailMsg = new Present<>("After check failed with: " + failMessage);
                return false;
            }
        }

        client.release();

        return true;
    }


    @Override
    public void describeTo(Description description)
    {

    }
}
