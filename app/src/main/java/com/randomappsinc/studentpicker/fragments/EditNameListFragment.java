package com.randomappsinc.studentpicker.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.IoniconsIcons;
import com.randomappsinc.studentpicker.R;
import com.randomappsinc.studentpicker.activities.MainActivity;
import com.randomappsinc.studentpicker.adapters.EditNameListAdapter;
import com.randomappsinc.studentpicker.adapters.NameCreationACAdapter;
import com.randomappsinc.studentpicker.database.DataSource;
import com.randomappsinc.studentpicker.dialogs.DeleteNameDialog;
import com.randomappsinc.studentpicker.dialogs.DuplicationDialog;
import com.randomappsinc.studentpicker.dialogs.MergeNameListsDialog;
import com.randomappsinc.studentpicker.dialogs.NameChoicesDialog;
import com.randomappsinc.studentpicker.dialogs.RenameDialog;
import com.randomappsinc.studentpicker.models.ListInfo;
import com.randomappsinc.studentpicker.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.Unbinder;

public class EditNameListFragment extends Fragment implements
        NameChoicesDialog.Listener, RenameDialog.Listener, DeleteNameDialog.Listener,
        DuplicationDialog.Listener, MergeNameListsDialog.Listener {

    @BindView(R.id.parent) View parent;
    @BindView(R.id.item_name_input) AutoCompleteTextView newNameInput;
    @BindView(R.id.no_content) TextView noContent;
    @BindView(R.id.num_names) TextView numNames;
    @BindView(R.id.content_list) ListView namesList;
    @BindView(R.id.plus_icon) ImageView plus;

    private EditNameListAdapter namesAdapter;
    private DataSource dataSource;
    private String listName;
    private NameChoicesDialog nameChoicesDialog;
    private RenameDialog renameDialog;
    private DeleteNameDialog deleteNameDialog;
    private DuplicationDialog duplicationDialog;
    private MergeNameListsDialog mergeNameListsDialog;
    private String[] importCandidates;
    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.lists_with_add_content, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        dataSource = new DataSource(getContext());

        newNameInput.setHint(R.string.name_hint);
        newNameInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        newNameInput.setAdapter(new NameCreationACAdapter(getActivity()));
        plus.setImageDrawable(new IconDrawable(
                getActivity(),
                IoniconsIcons.ion_android_add).colorRes(R.color.white));

        listName = getArguments().getString(MainActivity.LIST_NAME_KEY, "");
        importCandidates = dataSource.getAllNameListsMinusCurrent(listName);
        noContent.setText(R.string.no_names);

        namesAdapter = new EditNameListAdapter(noContent, numNames, listName);
        namesList.setAdapter(namesAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        nameChoicesDialog = new NameChoicesDialog(getActivity(), this);
        renameDialog = new RenameDialog(getActivity(), this);
        deleteNameDialog = new DeleteNameDialog(getActivity(), this);
        duplicationDialog = new DuplicationDialog(getActivity(), this);
        mergeNameListsDialog = new MergeNameListsDialog(getActivity(), this, importCandidates);
    }

    @OnClick(R.id.add_item)
    public void addItem() {
        String newName = newNameInput.getText().toString().trim();
        newNameInput.setText("");
        if (newName.isEmpty()) {
            UIUtils.showSnackbar(parent, getString(R.string.blank_name));
        } else {
            namesAdapter.addNames(newName, 1);
            String template = getString(R.string.added_name);
            UIUtils.showSnackbar(parent, String.format(template, newName));
        }
    }

    @OnItemClick(R.id.content_list)
    public void showNameOptions(final int position) {
        nameChoicesDialog.showChoices(namesAdapter.getItem(position));
    }

    @Override
    public void onRenameChosen(String name) {
        ListInfo listInfo = namesAdapter.getListInfo();
        int currentAmount = listInfo.getInstancesOfName(name);
        renameDialog.startRenamingProcess(name, currentAmount);
    }

    @Override
    public void onDeleteChosen(String name) {
        ListInfo listInfo = namesAdapter.getListInfo();
        int currentAmount = listInfo.getInstancesOfName(name);
        deleteNameDialog.startDeletionProcess(name, currentAmount);
    }

    @Override
    public void onDuplicationChosen(String name) {
        duplicationDialog.show(name);
    }

    @Override
    public void onRenameSubmitted(String previousName, String newName, int amountToRename) {
        dataSource.renamePeople(previousName, newName, listName, amountToRename);
        namesAdapter.changeName(previousName, newName, amountToRename);
    }

    @Override
    public void onDeletionSubmitted(String name, int amountToDelete) {
        namesAdapter.removeNames(name, amountToDelete);
        if (amountToDelete == 1) {
            String template = getString(R.string.deleted_name);
            UIUtils.showSnackbar(parent, String.format(template, name));
        } else {
            UIUtils.showSnackbar(parent, R.string.names_deleted);
        }
    }

    @Override
    public void onDuplicationSubmitted(String name, int amountToAdd) {
        dataSource.addNames(name, listName, amountToAdd);
        namesAdapter.addNames(name, amountToAdd);
        UIUtils.showSnackbar(parent, R.string.clones_added);
    }

    @Override
    public void onMergeSubmitted(List<String> listsToMergeIn) {
        ListInfo updatedListState = dataSource.getListInfo(listName);
        namesAdapter.importNamesFromList(updatedListState);
        UIUtils.showSnackbar(parent, getString(R.string.names_successfully_imported));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_name_list_menu, menu);
        UIUtils.loadMenuIcon(menu, R.id.import_names, IoniconsIcons.ion_android_upload, getActivity());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_names:
                if (importCandidates.length == 0) {
                    UIUtils.showSnackbar(parent, getString(R.string.no_name_lists_to_import));
                } else {
                    mergeNameListsDialog.show();
                }
                return true;
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
