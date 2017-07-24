// @flow
import { toastr } from 'react-redux-toastr';
import type { Channel } from 'redux-saga';
import { eventChannel } from 'redux-saga';
import { apply, cancelled, take } from 'redux-saga/effects';
import type { ApiError } from '../api/model';
import type { SevenWondersSession } from '../api/sevenWondersApi';

export default function* errorHandlingSaga(session: SevenWondersSession): * {
  const errorChannel: Channel<ApiError> = yield eventChannel(session.watchErrors());
  try {
    while (true) {
      const error: ApiError = yield take(errorChannel);
      yield* handleOneError(error);
    }
  } finally {
    if (yield cancelled()) {
      console.log('Error management saga cancelled');
      yield apply(errorChannel, errorChannel.close);
    }
  }
}

function* handleOneError(err: ApiError): * {
  console.error('Error received on web socket channel', err);
  const msg = buildMsg(err);
  yield apply(toastr, toastr.error, [msg, { icon: 'error' }]);
}

function buildMsg(err: ApiError): string {
  if (err.details.length > 0) {
    return err.details.map(d => d.message).join('\n');
  } else {
    return err.message;
  }
}
