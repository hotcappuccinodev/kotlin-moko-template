/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import Foundation
import UIKit
import MultiPlatformLibrary
import MultiPlatformLibraryMvvm
import MultiPlatformLibraryUnits

class NewsViewController: UIViewController {
    @IBOutlet private var tableView: UITableView!
    @IBOutlet private var activityIndicator: UIActivityIndicatorView!
    @IBOutlet private var emptyView: UIView!
    @IBOutlet private var errorView: UIView!
    @IBOutlet private var errorLabel: UILabel!
    
    private var viewModel: ListViewModel<News>!
    private var dataSource: TableUnitsSource!
    private var refreshControl: UIRefreshControl!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        viewModel = AppComponent.factory.newsFactory.createListViewModel()
        viewModel.onCreated()

        // binding methods from https://github.com/icerockdev/moko-mvvm
        activityIndicator.bindVisibility(liveData: viewModel.state.isLoadingState())
        tableView.bindVisibility(liveData: viewModel.state.isSuccessState())
        emptyView.bindVisibility(liveData: viewModel.state.isEmptyState())
        errorView.bindVisibility(liveData: viewModel.state.isErrorState())

        // in/out generics of Kotlin removed in swift, so we should map to valid class
        let errorText: LiveData<StringDesc> = viewModel.state.error()
            .map { $0 as? StringDesc ?? RawStringDesc(string: "") } as! LiveData<StringDesc>
        errorLabel.bindText(liveData: errorText)

        // datasource from https://github.com/icerockdev/moko-units
        dataSource = TableUnitsSourceKt.default(for: tableView)

        // manual bind to livedata, see https://github.com/icerockdev/moko-mvvm
        viewModel.state.data().addObserver { [weak self] itemsObject in
            guard let items = itemsObject as? [TableUnitItem] else { return }
            
            self?.dataSource.unitItems = items
            self?.tableView.reloadData()
        }
        
        refreshControl = UIRefreshControl()
        tableView.refreshControl = refreshControl
        refreshControl.addTarget(self, action: #selector(onRefresh), for: .valueChanged)
    }
    
    @IBAction func onRetryPressed() {
        viewModel.onRetryPressed()
    }
    
    @objc func onRefresh() {
        viewModel.onRefresh { [weak self] in
            self?.refreshControl.endRefreshing()
        }
    }
}
