package org.eclipse.scout.rt.client.transformation;

import org.eclipse.scout.rt.client.extension.ui.form.AbstractFormExtension;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormDisposeFormChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormInitFormChain;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.platform.BEANS;

public class FormExtension extends AbstractFormExtension<AbstractForm> {

  public FormExtension(AbstractForm ownerForm) {
    super(ownerForm);
  }

  @Override
  public void execInitForm(FormInitFormChain chain) {
    super.execInitForm(chain);
    BEANS.get(IDeviceTransformationService.class).getDeviceTransformer().transformForm(getOwner());
  }

  @Override
  public void execDisposeForm(FormDisposeFormChain chain) {
    super.execDisposeForm(chain);
    BEANS.get(IDeviceTransformationService.class).getDeviceTransformer().notifyFormDisposed(getOwner());
  }

}