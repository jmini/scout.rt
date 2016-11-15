package org.eclipse.scout.rt.client.ui.form.fields;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.IFormFieldVisitor;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_RadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_RadioButtonGroup.P_Button1;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_RadioButtonGroup.P_Button2;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab1;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab1.P_TreeBox;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab1.P_TreeBox.P_Seq;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab1.P_TreeBox.P_Seq.P_String;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab2.P_ListBox;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_GroupBox.P_TabBox.Tab2.P_ListBox.P_BigDec;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_InnerForm.MainBox;
import org.eclipse.scout.rt.client.ui.form.fields.FormFieldEnabledTest.P_OuterForm.MainBox.Wrapped;
import org.eclipse.scout.rt.client.ui.form.fields.bigdecimalfield.AbstractBigDecimalField;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractRadioButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBoxFilterBox.ActiveStateRadioButtonGroup.ActiveButton;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.IListBox;
import org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup.AbstractRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup.IRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.sequencebox.AbstractSequenceBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.AbstractTabBox;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.ITabBox;
import org.eclipse.scout.rt.client.ui.form.fields.treebox.AbstractTreeBox;
import org.eclipse.scout.rt.client.ui.form.fields.treebox.AbstractTreeBoxFilterBox.CheckedStateRadioButtonGroup.AllButton;
import org.eclipse.scout.rt.client.ui.form.fields.treebox.ITreeBox;
import org.eclipse.scout.rt.client.ui.form.fields.wrappedform.AbstractWrappedFormField;
import org.eclipse.scout.rt.shared.dimension.IDimensions;
import org.eclipse.scout.rt.testing.client.runner.ClientTestRunner;
import org.eclipse.scout.rt.testing.client.runner.RunWithClientSession;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <h3>{@link FormFieldEnabledTest}</h3>
 */
@RunWith(ClientTestRunner.class)
@RunWithSubject("default")
@RunWithClientSession(TestEnvironmentClientSession.class)
public class FormFieldEnabledTest {

  private ICompositeField createFixture() {
    ICompositeField result = new P_GroupBox();

    // verify structure
    Assert.assertEquals(2, result.getFieldCount());
    Assert.assertTrue(result.getFields().get(0) instanceof IRadioButtonGroup<?>);
    Assert.assertEquals(2, ((IRadioButtonGroup<?>) result.getFields().get(0)).getFieldCount());

    Assert.assertTrue(result.getFields().get(1) instanceof ITabBox);
    ITabBox tabbox = (ITabBox) result.getFields().get(1);
    Assert.assertEquals(2, tabbox.getFieldCount());

    IGroupBox tab1 = (IGroupBox) tabbox.getFields().get(0);
    IGroupBox tab2 = (IGroupBox) tabbox.getFields().get(1);
    Assert.assertEquals(1, tab1.getFieldCount());
    Assert.assertEquals(1, tab2.getFieldCount());

    ITreeBox<?> treebox = (ITreeBox<?>) tab1.getFields().get(0);
    IListBox<?> listbox = (IListBox<?>) tab2.getFields().get(0);
    Assert.assertEquals(2 /*treebox filter box is included as well*/, treebox.getFieldCount());
    Assert.assertEquals(2 /*listbox filter box is included as well*/, listbox.getFieldCount());

    return result;
  }

  @Test
  public void testWrappedFormField() {
    P_OuterForm frm = new P_OuterForm();
    final AtomicReference<P_String> ref = new AtomicReference<>();
    frm.visitFields(new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field instanceof P_String) {
          ref.set((P_String) field);
        }
        return ref.get() == null;
      }
    });
    P_String stringField = ref.get();
    Assert.assertNotNull(stringField);

    final AtomicInteger counter = new AtomicInteger(0);
    stringField.visitParents(new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        counter.incrementAndGet();
        return true;
      }
    });
    Assert.assertEquals(7, counter.intValue());

    MainBox innerMainBox = frm.getFieldByClass(Wrapped.class).getInnerForm().getFieldByClass(MainBox.class);
    final AtomicInteger counter2 = new AtomicInteger(0);
    innerMainBox.visitParents(new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        counter2.incrementAndGet();
        return true;
      }
    });
    Assert.assertEquals(2, counter2.intValue());
  }

  @Test
  public void testGetConfigured() {
    ICompositeField field = createFixture();
    Assert.assertTrue(field.isEnabled());
    Assert.assertTrue(field.isEnabledGranted());
    Assert.assertFalse(field.getFieldByClass(P_Button2.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Button2.class).isEnabledIncludingParents());
    Assert.assertTrue(field.getFieldByClass(P_Button2.class).isEnabledGranted());

    Assert.assertFalse(field.getFieldByClass(Tab1.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(Tab1.class).isEnabledGranted());

    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabledGranted());
    Assert.assertFalse(field.getFieldByClass(P_TreeBox.class).isEnabledIncludingParents());

    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabledGranted());
    Assert.assertFalse(field.getFieldByClass(P_Seq.class).isEnabledIncludingParents());
  }

  @Test
  public void testInheritanceComposite() {
    ICompositeField field = createFixture();
    field.getFieldByClass(P_RadioButtonGroup.class).setEnabledGranted(false);
    field.getFieldByClass(P_RadioButtonGroup.class).setEnabled(true);

    Assert.assertTrue(field.isEnabled());
    Assert.assertTrue(field.isEnabledGranted());
    Assert.assertFalse(field.getFieldByClass(P_RadioButtonGroup.class).isEnabledGranted());
    Assert.assertTrue(field.getFieldByClass(P_RadioButtonGroup.class).isEnabled(IDimensions.ENABLED));
    Assert.assertFalse(field.getFieldByClass(P_RadioButtonGroup.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_RadioButtonGroup.class).isEnabledIncludingParents());

    Assert.assertTrue(field.getFieldByClass(P_Button1.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Button1.class).isEnabledIncludingParents());
    field.getFieldByClass(P_Button1.class).setEnabled(false);

    Assert.assertFalse(field.getFieldByClass(P_Button2.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Button2.class).isEnabledIncludingParents());

    field.getFieldByClass(P_Button2.class).setEnabled(true);
    Assert.assertTrue(field.getFieldByClass(P_Button2.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Button2.class).isEnabledIncludingParents());

    field.getFieldByClass(P_Button2.class).setEnabledGranted(true, true);
    Assert.assertTrue(field.getFieldByClass(P_Button2.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_Button2.class).isEnabledIncludingParents());
    Assert.assertTrue(field.getFieldByClass(P_RadioButtonGroup.class).isEnabledGranted());
    Assert.assertTrue(field.getFieldByClass(P_RadioButtonGroup.class).isEnabled());

    Assert.assertTrue(field.getFieldByClass(P_Button1.class).isEnabledGranted());
    Assert.assertFalse(field.getFieldByClass(P_Button1.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Button1.class).isEnabledIncludingParents());
    field.getFieldByClass(P_Button1.class).setEnabled(true);
    Assert.assertTrue(field.getFieldByClass(P_Button1.class).isEnabledGranted());
    Assert.assertTrue(field.getFieldByClass(P_Button1.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_Button1.class).isEnabledIncludingParents());
  }

  @Test
  public void testPropagationToChildren() {
    ICompositeField field = createFixture();
    Assert.assertTrue(field.getFieldByClass(P_TabBox.class).isEnabled());
    field.getFieldByClass(P_TabBox.class).setEnabled(false, true, true);
    Assert.assertTrue(field.isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_TabBox.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(Tab1.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_TreeBox.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Seq.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_BigDec.class).isEnabled());
    field.setEnabled(false);
    Assert.assertFalse(field.isEnabled());
    field.getFieldByClass(P_TabBox.class).setEnabled(true, true, true);
    Assert.assertTrue(field.isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_TabBox.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(Tab1.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_BigDec.class).isEnabled());
  }

  @Test
  public void testInheritanceListBox() {
    ICompositeField field = createFixture();
    Assert.assertTrue(field.getFieldByClass(P_ListBox.class).isEnabled());
    field.getFieldByClass(P_ListBox.class).setEnabled(false, true);
    Assert.assertTrue(field.getFieldByClass(P_BigDec.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_BigDec.class).isEnabledIncludingParents());
    field.getFieldByClass(P_ListBox.class).setEnabled(true, true);
    Assert.assertTrue(field.getFieldByClass(P_BigDec.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_BigDec.class).isEnabledIncludingParents());
  }

  @Test
  public void testInheritanceTreeBox() {
    ICompositeField field = createFixture();
    Assert.assertFalse(field.getFieldByClass(Tab1.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_TreeBox.class).isEnabledIncludingParents());
    field.getFieldByClass(P_TreeBox.class).setEnabled(true, true);
    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabledIncludingParents());
    Assert.assertTrue(field.getFieldByClass(Tab1.class).isEnabled());

    Assert.assertTrue(field.getFieldByClass(P_TreeBox.class).isEnabled());
    field.getFieldByClass(P_TreeBox.class).setEnabled(false, true);
    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabled());
    Assert.assertFalse(field.getFieldByClass(P_Seq.class).isEnabledIncludingParents());
    field.getFieldByClass(P_TreeBox.class).setEnabled(true, true);
    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabled());
    Assert.assertTrue(field.getFieldByClass(P_Seq.class).isEnabledIncludingParents());
  }

  /**
   * Tests that the listbox-filterbox stays enabled even if the parent listbox gets disabled (filter should always be
   * usable).
   */
  @Test
  public void testListBoxFilterBoxEnabled() {
    ICompositeField field = createFixture();
    P_ListBox listBox = field.getFieldByClass(P_ListBox.class);
    listBox.setEnabled(false);
    Assert.assertFalse(listBox.isEnabled());
    Assert.assertTrue(listBox.getListBoxFilterBox().isEnabled());
    Assert.assertTrue(listBox.getListBoxFilterBox().getActiveStateRadioButtonGroup().getFieldByClass(ActiveButton.class).isEnabled());
  }

  /**
   * Tests that the treebox-filterbox stays enabled even if the parent treebox gets disabled (filter should always be
   * usable).
   */
  @Test
  public void testTreeBoxFilterBoxEnabled() {
    ICompositeField field = createFixture();
    P_TreeBox treeBox = field.getFieldByClass(P_TreeBox.class);
    treeBox.setEnabled(false);
    Assert.assertFalse(treeBox.isEnabled());
    Assert.assertTrue(treeBox.getTreeBoxFilterBox().isEnabled());
    Assert.assertTrue(treeBox.getTreeBoxFilterBox().getCheckedStateRadioButtonGroup().getFieldByClass(AllButton.class).isEnabled());
  }

  public static class P_InnerForm extends AbstractForm {
    public class MainBox extends P_GroupBox {
    }
  }

  public static class P_OuterForm extends AbstractForm {
    public class MainBox extends AbstractGroupBox {
      public class Wrapped extends AbstractWrappedFormField<P_InnerForm> {
        @Override
        protected Class<? extends IForm> getConfiguredInnerForm() {
          return P_InnerForm.class;
        }
      }
    }
  }

  public static class P_GroupBox extends AbstractGroupBox {

    public class P_RadioButtonGroup extends AbstractRadioButtonGroup<Long> {
      public class P_Button1 extends AbstractRadioButton<Long> {
      }

      public class P_Button2 extends AbstractRadioButton<Long> {
        @Override
        protected boolean getConfiguredEnabled() {
          return false;
        }
      }
    }

    public class P_TabBox extends AbstractTabBox {
      public class Tab1 extends AbstractGroupBox {

        @Override
        protected boolean getConfiguredEnabled() {
          return false;
        }

        public class P_TreeBox extends AbstractTreeBox<Long> {
          public class P_Seq extends AbstractSequenceBox {
            public class P_String extends AbstractStringField {
            }
          }
        }
      }

      public class Tab2 extends AbstractGroupBox {
        public class P_ListBox extends AbstractListBox {

          public class P_BigDec extends AbstractBigDecimalField {
          }
        }
      }
    }
  }
}